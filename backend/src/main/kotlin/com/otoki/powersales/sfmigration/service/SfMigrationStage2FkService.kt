package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.dto.SubstepResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 2-A FK Resolve — sfid 기반 FK id 컬럼 채움.
 *
 * 본 substep 은 다른 substep 과 다르게 schema 동적 스캔이 필요하다 (어떤 entity 가 어떤
 * `*_sfid` 컬럼을 가지는지 information_schema 로 발견).
 *
 * 처리 흐름:
 *   1. powersales schema 의 모든 `*_sfid` 컬럼 발견
 *   2. 각 컬럼마다 deriveFkResolveSpec() 로 FK spec 도출
 *   3. 짝의 `<prefix>_id` 컬럼이 존재하면 UPDATE, 부재면 정책 위반 보고
 *   4. POLYMORPHIC_OWNER_TABLES 에 대해 owner_sfid prefix=00G → owner_group_id 분기
 */
@Service
class SfMigrationStage2FkService(
    @PersistenceContext private val em: EntityManager,
) {

    private val schemaName = "powersales"

    @Transactional
    fun runFkResolve(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        val errors = mutableListOf<String>()

        // 1. powersales schema 의 모든 `*_sfid` 컬럼 발견 (table, column) 페어.
        val sfidColumns = listSfidColumns()
        var totalUpdated = 0

        // 2. 각 (table, sfidColumn) 처리.
        for ((tableName, sfidColumn) in sfidColumns) {
            val spec = deriveFkResolveSpec(sfidColumn) ?: continue
            val updated = applyOneFkResolve(tableName, spec, errors)
            if (updated >= 0) {
                totalUpdated += updated
                results += SubstepResult(
                    label = "$tableName.$sfidColumn -> ${spec.idColumn}",
                    rowsAffected = updated,
                )
            }
        }

        // 3. Polymorphic owner_group_id 분기.
        for (tableName in POLYMORPHIC_OWNER_TABLES) {
            if (!columnExists(tableName, "owner_user_id")) continue
            if (!columnExists(tableName, "owner_group_id")) continue
            val n = applyPolymorphicOwnerGroup(tableName)
            totalUpdated += n
            results += SubstepResult(
                label = "$tableName.owner_sfid (00G) -> owner_group_id",
                rowsAffected = n,
            )
        }

        if (errors.isNotEmpty()) {
            results += SubstepResult(
                label = "WARNINGS",
                rowsAffected = errors.size,
            )
        }

        return SfMigrationStage2Response(
            substep = "fk",
            results = results,
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * powersales schema 에 존재하는 모든 `*_sfid` 컬럼을 (table, column) 페어로 반환.
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

    /**
     * 한 FK resolve 적용. 음수 반환 = 적용 skip + 사유 기록.
     */
    private fun applyOneFkResolve(
        tableName: String,
        spec: FkResolveSpec,
        errors: MutableList<String>,
    ): Int {
        if (!columnExists(tableName, spec.idColumn)) {
            errors.add("[$tableName] ${spec.sfidColumn} 존재하나 짝의 ${spec.idColumn} FK 컬럼 부재 — 스키마 정합 위반")
            return -1
        }
        if (!columnExists(spec.refTable, spec.refIdColumn)) {
            errors.add("[$tableName] ${spec.sfidColumn} → ${spec.refTable}.${spec.refIdColumn} — 대상 테이블/컬럼 부재")
            return -1
        }
        val quotedTable = quoteTable(tableName)
        val quotedRefTable = quoteTable(spec.refTable)
        val sql = """
            UPDATE $schemaName.$quotedTable t
            SET ${spec.idColumn} = r.${spec.refIdColumn}
            FROM $schemaName.$quotedRefTable r
            WHERE t.${spec.sfidColumn} IS NOT NULL
              AND t.${spec.idColumn} IS NULL
              AND r.${spec.refSfidColumn} = t.${spec.sfidColumn}
        """.trimIndent()
        return em.createNativeQuery(sql).executeUpdate()
    }

    /**
     * owner_sfid prefix `00G` (Group) → owner_group_id 채움.
     * STD audit FK 가 잘못 owner_user_id 에 채운 row (00G prefix) 를 사전 NULL 정정 후 Group 쪽 채움.
     */
    private fun applyPolymorphicOwnerGroup(tableName: String): Int {
        val quotedTable = quoteTable(tableName)
        // 1) 잘못 채운 User FK 정정
        em.createNativeQuery(
            """
            UPDATE $schemaName.$quotedTable
            SET owner_user_id = NULL
            WHERE owner_sfid IS NOT NULL
              AND owner_sfid LIKE '00G%'
              AND owner_user_id IS NOT NULL
            """.trimIndent()
        ).executeUpdate()

        // 2) Group 쪽 채움
        return em.createNativeQuery(
            """
            UPDATE $schemaName.$quotedTable t
            SET owner_group_id = g.group_id
            FROM $schemaName."group" g
            WHERE t.owner_sfid IS NOT NULL
              AND t.owner_sfid LIKE '00G%'
              AND t.owner_group_id IS NULL
              AND g.sfid = t.owner_sfid
            """.trimIndent()
        ).executeUpdate()
    }

    private fun quoteTable(name: String): String = if (name == "user" || name == "group") "\"$name\"" else name
}
