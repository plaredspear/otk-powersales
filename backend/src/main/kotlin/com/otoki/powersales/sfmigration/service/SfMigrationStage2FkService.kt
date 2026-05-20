package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.dto.SubstepResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Stage 2-A FK Resolve — sfid 기반 FK id 컬럼 채움 (테이블 단위 + chunk 처리).
 *
 * 처리 흐름:
 *   1. powersales schema 의 모든 `*_sfid` 컬럼 발견 → 테이블 단위 groupBy
 *   2. 테이블마다 information_schema 로 PK 컬럼 발견 + MAX(pk) 조회
 *   3. PK 범위 chunk (기본 100K) 로 페이징하며 한 chunk 안에서 모든 FK 컬럼 LEFT JOIN 단일 UPDATE
 *   4. polymorphic owner_sfid (`005` User / `00G` Group) 는 같은 UPDATE 의 SET 절 안에 CASE 분기로 흡수
 *   5. 각 chunk 는 TransactionTemplate 으로 별도 commit → 부분 실패 안전 + 재호출 멱등 (id IS NULL 조건)
 */
@Service
class SfMigrationStage2FkService(
    @PersistenceContext private val em: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val schemaName = "powersales"
    private val chunkSize = 100_000L

    fun runFkResolve(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        val errors = mutableListOf<String>()

        // 1. 테이블 단위 groupBy (sfid 컬럼 + FK spec 도출).
        val plansByTable = buildPlansByTable(errors)
        log.info("[fk] start — {} tables to resolve", plansByTable.size)

        // 2. 테이블 단위 처리.
        var totalUpdated = 0
        for ((tableName, plan) in plansByTable) {
            val updated = applyOneTableFkResolve(tableName, plan, errors)
            totalUpdated += updated
            results += SubstepResult(
                label = "$tableName (${plan.columns.size} FK${if (plan.polymorphicOwner) " + polymorphic owner" else ""})",
                rowsAffected = updated,
            )
        }

        if (errors.isNotEmpty()) {
            errors.forEach { log.warn("[fk] WARNING: {}", it) }
            results += SubstepResult(label = "WARNINGS", rowsAffected = errors.size)
        }

        log.info("[fk] done — total {} rows updated across {} tables", totalUpdated, plansByTable.size)
        return SfMigrationStage2Response(
            substep = "fk",
            results = results,
            totalRowsAffected = totalUpdated,
        )
    }

    internal data class ColumnPlan(
        val sfidColumn: String,
        val spec: FkResolveSpec,
        val joinAlias: String,
    )

    internal data class TablePlan(
        val columns: List<ColumnPlan>,
        val polymorphicOwner: Boolean,
        val pkColumn: String,
    )

    /**
     * 모든 `*_sfid` 컬럼을 테이블 단위로 묶어 실행 계획 도출.
     * spec / 짝의 id 컬럼 / ref table 존재 여부 검증 후 실패 항목은 errors 에 기록하고 skip.
     */
    private fun buildPlansByTable(errors: MutableList<String>): Map<String, TablePlan> {
        val sfidColumns = listSfidColumns()
        val byTable = sfidColumns.groupBy({ it.first }, { it.second })

        return byTable.mapNotNull { (tableName, cols) ->
            val pkColumn = findPkColumn(tableName)
            if (pkColumn == null) {
                errors.add("[$tableName] PK 컬럼 발견 실패 — chunk 페이징 불가, skip")
                return@mapNotNull null
            }

            val columns = cols.mapNotNull { sfidColumn ->
                val spec = deriveFkResolveSpec(sfidColumn) ?: return@mapNotNull null
                if (!columnExists(tableName, spec.idColumn)) {
                    errors.add("[$tableName] $sfidColumn 존재하나 짝의 ${spec.idColumn} FK 컬럼 부재")
                    return@mapNotNull null
                }
                if (!columnExists(spec.refTable, spec.refIdColumn)) {
                    errors.add("[$tableName] $sfidColumn → ${spec.refTable}.${spec.refIdColumn} — 대상 테이블/컬럼 부재")
                    return@mapNotNull null
                }
                ColumnPlan(
                    sfidColumn = sfidColumn,
                    spec = spec,
                    joinAlias = "j_${spec.sfidColumn.removeSuffix("_sfid")}",
                )
            }

            val polymorphic = tableName in POLYMORPHIC_OWNER_TABLES &&
                columnExists(tableName, "owner_user_id") &&
                columnExists(tableName, "owner_group_id") &&
                columnExists(tableName, "owner_sfid")

            if (columns.isEmpty() && !polymorphic) return@mapNotNull null
            tableName to TablePlan(columns, polymorphic, pkColumn)
        }.toMap()
    }

    /**
     * 한 테이블의 모든 FK 를 chunk 페이징으로 LEFT JOIN 단일 UPDATE 처리.
     */
    private fun applyOneTableFkResolve(
        tableName: String,
        plan: TablePlan,
        errors: MutableList<String>,
    ): Int {
        val maxPk = findMaxPk(tableName, plan.pkColumn)
        if (maxPk == 0L) {
            log.info("[fk] {} empty — skip", tableName)
            return 0
        }

        val sql = buildChunkUpdateSql(tableName, plan)
        var totalUpdated = 0
        var lastPk = 0L
        var chunkNo = 0
        val chunkCount = ((maxPk + chunkSize - 1) / chunkSize).coerceAtLeast(1)

        log.info(
            "[fk] {} start — maxPk={}, {} chunks of {} rows ({} FK{})",
            tableName, maxPk, chunkCount, chunkSize,
            plan.columns.size,
            if (plan.polymorphicOwner) " + polymorphic owner" else "",
        )

        while (lastPk < maxPk) {
            val upperPk = lastPk + chunkSize
            val started = System.currentTimeMillis()
            val n = try {
                transactionTemplate.execute {
                    em.createNativeQuery(sql)
                        .setParameter("lastPk", lastPk)
                        .setParameter("upperPk", upperPk)
                        .executeUpdate()
                } ?: 0
            } catch (e: Exception) {
                errors.add("[$tableName] chunk #${chunkNo + 1} (pk ${lastPk + 1}~$upperPk) 실패: ${e.message}")
                log.error("[fk] {} chunk #{} 실패 — pk {}~{}", tableName, chunkNo + 1, lastPk + 1, upperPk, e)
                lastPk = upperPk
                chunkNo++
                continue
            }

            totalUpdated += n
            chunkNo++
            log.info(
                "[fk] {} chunk {}/{} pk {}~{} → {} rows ({} ms, total {})",
                tableName, chunkNo, chunkCount, lastPk + 1, upperPk, n,
                System.currentTimeMillis() - started, totalUpdated,
            )
            lastPk = upperPk
        }

        return totalUpdated
    }

    /**
     * 테이블 단위 LEFT JOIN UPDATE SQL 빌더.
     *
     * SET 절: 컬럼별 COALESCE(t.<id>, j_<...>.<refId>).
     * polymorphic owner_sfid: CASE WHEN owner_sfid LIKE '005%' / '00G%' 분기.
     * WHERE: PK 범위 + 적어도 한 FK 가 NULL 인 row 만 대상 (재호출 멱등).
     */
    internal fun buildChunkUpdateSql(tableName: String, plan: TablePlan): String {
        val quotedTable = quoteIdent(tableName)

        val setClauses = mutableListOf<String>()
        val joinClauses = mutableListOf<String>()
        val whereOrClauses = mutableListOf<String>()

        for (col in plan.columns) {
            // owner_sfid 가 있고 polymorphic 테이블이면, deriveFkResolveSpec 의 단일 user 매핑은
            // polymorphic 분기로 대체 (아래 별도 처리). 일반 owner FK 가 polymorphic 테이블에서는
            // 중복 처리되지 않도록 owner_sfid 컬럼은 polymorphic 블록에 위임.
            if (plan.polymorphicOwner && col.sfidColumn == "owner_sfid") continue

            val refTable = quoteIdent(col.spec.refTable)
            val alias = col.joinAlias
            setClauses += "${col.spec.idColumn} = COALESCE(t.${col.spec.idColumn}, $alias.${col.spec.refIdColumn})"
            joinClauses += "LEFT JOIN $schemaName.$refTable $alias " +
                "ON $alias.${col.spec.refSfidColumn} = src.${col.sfidColumn}"
            whereOrClauses += "(t.${col.spec.idColumn} IS NULL AND src.${col.sfidColumn} IS NOT NULL)"
        }

        if (plan.polymorphicOwner) {
            val userAlias = "j_owner_user"
            val groupAlias = "j_owner_group"
            setClauses += "owner_user_id = CASE WHEN src.owner_sfid LIKE '005%' " +
                "THEN COALESCE(t.owner_user_id, $userAlias.user_id) ELSE t.owner_user_id END"
            setClauses += "owner_group_id = CASE WHEN src.owner_sfid LIKE '00G%' " +
                "THEN COALESCE(t.owner_group_id, $groupAlias.group_id) ELSE t.owner_group_id END"
            joinClauses += "LEFT JOIN $schemaName.\"user\" $userAlias " +
                "ON $userAlias.sfid = src.owner_sfid AND src.owner_sfid LIKE '005%'"
            joinClauses += "LEFT JOIN $schemaName.\"group\" $groupAlias " +
                "ON $groupAlias.sfid = src.owner_sfid AND src.owner_sfid LIKE '00G%'"
            whereOrClauses += "(t.owner_user_id IS NULL AND src.owner_sfid LIKE '005%')"
            whereOrClauses += "(t.owner_group_id IS NULL AND src.owner_sfid LIKE '00G%')"
        }

        return """
            UPDATE $schemaName.$quotedTable t
            SET ${setClauses.joinToString(",\n                ")}
            FROM $schemaName.$quotedTable src
              ${joinClauses.joinToString("\n              ")}
            WHERE t.${plan.pkColumn} = src.${plan.pkColumn}
              AND src.${plan.pkColumn} > :lastPk
              AND src.${plan.pkColumn} <= :upperPk
              AND (
                ${whereOrClauses.joinToString("\n                OR ")}
              )
        """.trimIndent()
    }

    /**
     * powersales schema 의 모든 `*_sfid` 컬럼을 (table, column) 페어로 반환.
     */
    private fun listSfidColumns(): List<Pair<String, String>> {
        val sql = """
            SELECT table_name, column_name
            FROM information_schema.columns
            WHERE table_schema = :schema
              AND column_name LIKE '%\_sfid' ESCAPE '\'
            ORDER BY table_name, column_name
        """.trimIndent()
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(sql)
            .setParameter("schema", schemaName)
            .resultList as List<Array<Any>>
        return rows.map { it[0].toString() to it[1].toString() }
    }

    /**
     * 단일 PK 컬럼명 발견 (composite PK 는 미지원 — null 반환 + skip).
     */
    private fun findPkColumn(tableName: String): String? {
        val sql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON kcu.constraint_name = tc.constraint_name
             AND kcu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = :schema
              AND tc.table_name = :table
            ORDER BY kcu.ordinal_position
        """.trimIndent()
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(sql)
            .setParameter("schema", schemaName)
            .setParameter("table", tableName)
            .resultList as List<Any>
        if (rows.isEmpty()) return null
        if (rows.size > 1) return null // composite PK 미지원
        return rows[0].toString()
    }

    private fun findMaxPk(tableName: String, pkColumn: String): Long {
        val quotedTable = quoteIdent(tableName)
        val sql = "SELECT COALESCE(MAX($pkColumn), 0) FROM $schemaName.$quotedTable"
        val result = em.createNativeQuery(sql).singleResult ?: return 0L
        return (result as Number).toLong()
    }

    private fun columnExists(tableName: String, columnName: String): Boolean {
        val sql = """
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = :schema AND table_name = :table AND column_name = :col
        """.trimIndent()
        val rows = em.createNativeQuery(sql)
            .setParameter("schema", schemaName)
            .setParameter("table", tableName)
            .setParameter("col", columnName)
            .resultList
        return rows.isNotEmpty()
    }

    private fun quoteIdent(name: String): String = if (name == "user" || name == "group") "\"$name\"" else name
}
