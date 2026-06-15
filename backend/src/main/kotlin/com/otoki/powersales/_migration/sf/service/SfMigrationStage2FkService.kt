package com.otoki.powersales._migration.sf.service

import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Table
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
    private val progress: SfFkResolveProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val schemaName = "powersales"
    private val chunkSize = 100_000L

    /**
     * 처리 가능한 테이블 목록 (SF Migration 페이지 드롭다운용). buildPlansByTable 의 키 집합과 동일한
     * 도출 로직을 거치되, errors 는 버리고 테이블명만 정렬해 반환한다.
     *
     * [HEROKU_TABLES_WITH_SF_SFID] (Heroku 전용이나 sfid 가 SF Id 인 테이블) 는 SF 페이지가 아니라
     * Heroku Migration 페이지에 노출하므로 ([listHerokuSfidResolvableTables]) 여기서는 제외한다.
     * (실행 엔진 [runFkResolve] 자체는 두 테이블을 동일하게 처리 — UI 소속만 분리.)
     */
    fun listResolvableTables(): List<String> {
        val throwaway = mutableListOf<String>()
        return buildPlansByTable(throwaway).keys
            .filterNot { it in HEROKU_TABLES_WITH_SF_SFID }
            .sorted()
    }

    /**
     * Heroku Migration 페이지의 sfid FK Resolve 드롭다운용 목록.
     *
     * [HEROKU_TABLES_WITH_SF_SFID] (safety_check_submission / product_expiration 등 — `@HerokuOnly`
     * 이지만 `_sfid` 값이 진짜 SF Id) 중 실제 처리 계획이 도출된 테이블만 정렬해 반환한다.
     * 실행은 SF 와 동일하게 [runFkResolve] (targetTable) 를 재사용한다.
     */
    fun listHerokuSfidResolvableTables(): List<String> {
        val throwaway = mutableListOf<String>()
        return buildPlansByTable(throwaway).keys
            .filter { it in HEROKU_TABLES_WITH_SF_SFID }
            .sorted()
    }

    /**
     * @param targetTable null 이면 전체 테이블, 지정 시 해당 테이블 1개만 처리.
     *   처리 가능한 테이블 집합 밖이면 IllegalArgumentException.
     */
    fun runFkResolve(targetTable: String? = null): SfMigrationStage2Response =
        runFkResolveForTables(targetTable?.let { setOf(it) })

    /**
     * 여러 테이블을 한 progress 안에서 일괄 처리.
     *
     * @param targetTables null 이면 전체 테이블, 비어있지 않은 집합이면 그 테이블들만 한 progress 로 처리.
     *   Heroku sfid 페이지 ([HEROKU_TABLES_WITH_SF_SFID]) 가 전체-실행 시 진행 상태를 한 묶음으로
     *   보이게 하기 위함 (테이블마다 progress.begin 으로 리셋되는 것 회피). 처리 가능 집합 밖이면 throw.
     */
    fun runFkResolveForTables(targetTables: Set<String>? = null): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        val errors = mutableListOf<String>()

        // 1. 테이블 단위 groupBy (sfid 컬럼 + FK spec 도출).
        val allPlans = buildPlansByTable(errors)
        val plansByTable = if (targetTables == null) {
            allPlans
        } else {
            targetTables.associateWith { t ->
                allPlans[t] ?: throw IllegalArgumentException(
                    "처리 가능한 테이블이 아닙니다: $t (대상: ${allPlans.keys.sorted()})",
                )
            }
        }
        // 부분 실행 시 buildPlansByTable 가 대상 외 테이블에 대해 남긴 도출 경고는 무의미하므로
        // 대상 테이블 접두(`[tableName]`) 경고만 남긴다.
        if (targetTables != null) {
            val prefixes = targetTables.map { "[$it]" }
            val kept = errors.filter { e -> prefixes.any { e.startsWith(it) } }
            errors.clear()
            errors.addAll(kept)
        }
        log.info(
            "[fk] start — {} tables to resolve{}",
            plansByTable.size,
            if (targetTables != null) " (subset: ${targetTables.sorted()})" else "",
        )
        progress.begin(totalTables = plansByTable.size)
        errors.forEach { progress.addError(it) }

        // 2. 테이블 단위 처리.
        var totalUpdated = 0
        var totalDangling = 0
        try {
            for ((tableName, plan) in plansByTable) {
                val updated = applyOneTableFkResolve(tableName, plan, errors)
                totalUpdated += updated
                val result = SubstepResult(
                    label = "$tableName (${plan.columns.size} FK${buildString {
                if (plan.polymorphicOwner) append(" + polymorphic owner")
                if (plan.polymorphicRelated) append(" + polymorphic related")
            }})",
                    rowsAffected = updated,
                )
                results += result
                progress.finishTable(result)

                // dangling 측정 — resolve 후에도 *_id 가 NULL 인데 *_sfid 는 NOT NULL 인 row.
                // (부모 row 미적재 / 부모가 export 대상 아님 / sfid prefix 가 예상 범위 밖 등)
                // 자연 키 substep 과 동일하게 잔존 건수를 측정해 가시성 확보.
                totalDangling += reportDangling(tableName, plan, results, errors)
            }

            if (errors.isNotEmpty()) {
                errors.forEach { log.warn("[fk] WARNING: {}", it) }
                results += SubstepResult(label = "WARNINGS", rowsAffected = errors.size)
            }

            log.info(
                "[fk] done — total {} rows updated across {} tables ({} dangling unresolved)",
                totalUpdated, plansByTable.size, totalDangling,
            )
            progress.finishOk()
        } catch (e: Exception) {
            log.error("[fk] aborted with unhandled exception", e)
            progress.finishWithFailure(e.message ?: e.javaClass.simpleName)
            throw e
        }

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
        // spec #782 P2-B — Group.related_sfid prefix 005/00E 분기 (User/UserRole typed FK).
        // 기존 테스트 호출 호환성 위해 default false — POLYMORPHIC_RELATED_TABLES 화이트리스트로 활성화.
        val polymorphicRelated: Boolean = false,
    )

    /**
     * 모든 `*_sfid` 컬럼을 테이블 단위로 묶어 실행 계획 도출.
     * spec / 짝의 id 컬럼 / ref table 존재 여부 검증 후 실패 항목은 errors 에 기록하고 skip.
     */
    private fun buildPlansByTable(errors: MutableList<String>): Map<String, TablePlan> {
        val sfidColumns = listSfidColumns()
        val byTable = sfidColumns.groupBy({ it.first }, { it.second })

        // `@HerokuOnly` 전용 테이블은 SF FK Resolve 대상에서 제외 (HEROKU_TABLES_WITH_SF_SFID 예외).
        // SF FK Resolve 가 `*_sfid` 컬럼을 무차별 스캔하므로 Heroku 전용 테이블이 끌려오는데, 그 값이
        // SF Id 가 아니거나 SF substep 이 건드려선 안 되는 테이블을 막는다.
        val excludedHerokuTables = herokuOnlyTablesExcludedFromSfFkResolve()

        return byTable.mapNotNull { (tableName, cols) ->
            if (tableName in excludedHerokuTables) {
                log.info("[fk] {} — @HerokuOnly 전용 테이블, SF FK Resolve 제외", tableName)
                return@mapNotNull null
            }
            val pkColumn = findPkColumn(tableName)
            if (pkColumn == null) {
                errors.add("[$tableName] PK 컬럼 발견 실패 — chunk 페이징 불가, skip")
                return@mapNotNull null
            }

            val columns = cols.mapNotNull { sfidColumn ->
                val spec = deriveFkResolveSpec(sfidColumn, tableName) ?: return@mapNotNull null
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

            val polymorphicRelated = tableName in POLYMORPHIC_RELATED_TABLES &&
                columnExists(tableName, "related_user_id") &&
                columnExists(tableName, "related_user_role_id") &&
                columnExists(tableName, "related_sfid")

            if (columns.isEmpty() && !polymorphic && !polymorphicRelated) return@mapNotNull null
            tableName to TablePlan(columns, polymorphic, pkColumn, polymorphicRelated)
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
            buildString {
                if (plan.polymorphicOwner) append(" + polymorphic owner")
                if (plan.polymorphicRelated) append(" + polymorphic related")
            },
        )
        progress.beginTable(tableName, chunkCount.toInt())

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
                val message = "[$tableName] chunk #${chunkNo + 1} (pk ${lastPk + 1}~$upperPk) 실패: ${e.message}"
                errors.add(message)
                progress.addError(message)
                log.error("[fk] {} chunk #{} 실패 — pk {}~{}", tableName, chunkNo + 1, lastPk + 1, upperPk, e)
                progress.advanceChunk(0)
                lastPk = upperPk
                chunkNo++
                continue
            }

            totalUpdated += n
            chunkNo++
            progress.advanceChunk(n)
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
     * resolve 후 dangling (미해소) FK 잔존 건수를 측정해 WARN 로그 + results 에 노출.
     *
     * 일반 FK: `*_id IS NULL AND *_sfid IS NOT NULL` 건수.
     * polymorphic owner: `owner_user_id IS NULL AND owner_group_id IS NULL AND owner_sfid IS NOT NULL` 건수
     *   (005/00G 어느 prefix 에도 매칭 안 된 row 포함 — 예상 prefix 밖도 흡수).
     * polymorphic related: `related_user_id IS NULL AND related_user_role_id IS NULL AND related_sfid IS NOT NULL`.
     *
     * dangling 0 이면 로그/results 에 추가하지 않음 (정상). 0 초과만 WARN.
     *
     * @return 이 테이블의 dangling 총합.
     */
    private fun reportDangling(
        tableName: String,
        plan: TablePlan,
        results: MutableList<SubstepResult>,
        errors: MutableList<String>,
    ): Int {
        var tableDangling = 0

        for (col in plan.columns) {
            if (plan.polymorphicOwner && col.sfidColumn == "owner_sfid") continue
            if (plan.polymorphicRelated && col.sfidColumn == "related_sfid") continue
            val n = countDangling(tableName, "${col.spec.idColumn} IS NULL AND ${col.sfidColumn} IS NOT NULL")
            if (n > 0) {
                tableDangling += n
                val message = "[$tableName] ${col.sfidColumn} → ${col.spec.idColumn} 미해소 $n 건 " +
                    "(${col.spec.refTable} lookup 실패 — 부모 미적재 / export 대상 외 / prefix 불일치)"
                errors.add(message)
                progress.addError(message)
                results += SubstepResult(label = "DANGLING $tableName.${col.spec.idColumn}", rowsAffected = n)
            }
        }

        if (plan.polymorphicOwner) {
            val n = countDangling(
                tableName,
                "owner_user_id IS NULL AND owner_group_id IS NULL AND owner_sfid IS NOT NULL",
            )
            if (n > 0) {
                tableDangling += n
                val message = "[$tableName] owner_sfid → owner_user_id/owner_group_id 미해소 $n 건 " +
                    "(005/00G prefix 미매칭 또는 부모 미적재)"
                errors.add(message)
                progress.addError(message)
                results += SubstepResult(label = "DANGLING $tableName.owner", rowsAffected = n)
            }
        }

        if (plan.polymorphicRelated) {
            val n = countDangling(
                tableName,
                "related_user_id IS NULL AND related_user_role_id IS NULL AND related_sfid IS NOT NULL",
            )
            if (n > 0) {
                tableDangling += n
                val message = "[$tableName] related_sfid → related_user_id/related_user_role_id 미해소 $n 건 " +
                    "(005/00E prefix 미매칭 또는 부모 미적재)"
                errors.add(message)
                progress.addError(message)
                results += SubstepResult(label = "DANGLING $tableName.related", rowsAffected = n)
            }
        }

        return tableDangling
    }

    private fun countDangling(tableName: String, whereClause: String): Int {
        val quotedTable = quoteIdent(tableName)
        val sql = "SELECT COUNT(*) FROM $schemaName.$quotedTable WHERE $whereClause"
        val result = em.createNativeQuery(sql).singleResult ?: return 0
        return (result as Number).toInt()
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
            // 동일 패턴 — related_sfid 도 polymorphicRelated 블록에 위임.
            if (plan.polymorphicRelated && col.sfidColumn == "related_sfid") continue

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

        if (plan.polymorphicRelated) {
            // spec #782 P2-B — Group.related_sfid prefix 005 → related_user_id, 00E → related_user_role_id.
            // SF describe `Group.RelatedId.referenceTo = [User, UserRole]` 정합.
            val userAlias = "j_related_user"
            val userRoleAlias = "j_related_user_role"
            setClauses += "related_user_id = CASE WHEN src.related_sfid LIKE '005%' " +
                "THEN COALESCE(t.related_user_id, $userAlias.user_id) ELSE t.related_user_id END"
            setClauses += "related_user_role_id = CASE WHEN src.related_sfid LIKE '00E%' " +
                "THEN COALESCE(t.related_user_role_id, $userRoleAlias.user_role_id) ELSE t.related_user_role_id END"
            joinClauses += "LEFT JOIN $schemaName.\"user\" $userAlias " +
                "ON $userAlias.sfid = src.related_sfid AND src.related_sfid LIKE '005%'"
            joinClauses += "LEFT JOIN $schemaName.user_role $userRoleAlias " +
                "ON $userRoleAlias.sfid = src.related_sfid AND src.related_sfid LIKE '00E%'"
            whereOrClauses += "(t.related_user_id IS NULL AND src.related_sfid LIKE '005%')"
            whereOrClauses += "(t.related_user_role_id IS NULL AND src.related_sfid LIKE '00E%')"
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

    /**
     * JPA metamodel 을 스캔해 `@HerokuOnly` 엔티티의 물리 테이블명을 모으고, SF FK Resolve 가
     * 예외적으로 처리해야 하는 [HEROKU_TABLES_WITH_SF_SFID] 는 제외해 "SF substep 에서 건드리면
     * 안 되는 Heroku 전용 테이블" 집합을 반환한다.
     *
     * 신규 `@HerokuOnly` 테이블이 추가돼도 자동으로 제외되며, 그 값이 SF Id 라 처리가 필요하면
     * HEROKU_TABLES_WITH_SF_SFID 에만 등록하면 된다.
     */
    private fun herokuOnlyTablesExcludedFromSfFkResolve(): Set<String> {
        return em.metamodel.entities
            .mapNotNull { it.javaType.getAnnotation(HerokuOnly::class.java)?.let { _ -> it.javaType } }
            .mapNotNull { it.getAnnotation(Table::class.java)?.name?.takeIf { n -> n.isNotBlank() } }
            .filterNot { it in HEROKU_TABLES_WITH_SF_SFID }
            .toSet()
    }
}
