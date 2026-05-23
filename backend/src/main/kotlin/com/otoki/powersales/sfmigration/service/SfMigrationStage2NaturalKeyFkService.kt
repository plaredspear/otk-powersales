package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.dto.SubstepResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 2-B Natural Key FK Resolve — sfid 가 아닌 자연 키 (developer_name / name / sfid 컬럼) 기반 FK id 채움.
 *
 * `NATURAL_KEY_FK_MAPPINGS` 의 9 entry 일괄 SQL UPDATE 적용 — sfid prefix path 와 분리.
 *
 * ## 결정 사항 정합 (spec #800 Q1~Q5 옵션 1)
 * - Q1: Service 분리 — `SfMigrationStage2FkService` 와 책임 분리
 * - Q2: Substep `fk-natural-key` — Controller 에 별도 endpoint
 * - Q3: 단순 UPDATE — 식별자 모두 컴파일 타임 상수 (SQL injection 없음)
 * - Q4: 매칭 부재 row 는 NULL 유지 + WARN log
 * - Q5: WHERE 절에 `AND s.<targetIdColumn> IS NULL` — 멱등성 (재실행 시 0 rows updated)
 *
 * ## 동작 흐름
 * 1. `NATURAL_KEY_FK_MAPPINGS` 순회
 * 2. 각 spec 별 `UPDATE source s SET target_id = r.target_id FROM ref r WHERE r.refColumn = s.sourceColumn AND s.target_id IS NULL`
 * 3. 결과 row 수 + 영향 받지 못한 row 수 (label 별 SubstepResult)
 */
@Service
class SfMigrationStage2NaturalKeyFkService(
    @PersistenceContext private val em: EntityManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val schemaName = "powersales"

    @Transactional
    fun runNaturalKeyFkResolve(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        var totalUpdated = 0

        log.info("[fk-natural-key] start — {} mappings", NATURAL_KEY_FK_MAPPINGS.size)

        for (spec in NATURAL_KEY_FK_MAPPINGS) {
            val quotedSource = quoteIdentifier(spec.sourceTable)
            val quotedRef = quoteIdentifier(spec.refTable)

            // 매칭 부재 row count 사전 측정 (Q4 — WARN log 용)
            val unmatchedBefore = countUnmatched(spec, quotedSource, quotedRef)

            val sql = buildUpdateSql(spec)

            val n = em.createNativeQuery(sql).executeUpdate()
            totalUpdated += n

            val label = "${spec.sourceTable}.${spec.sourceColumn} → ${spec.refTable}.${spec.targetIdColumn}"
            results += SubstepResult(label = label, rowsAffected = n)

            log.info("[fk-natural-key] {} : updated={} (unmatched before={})", label, n, unmatchedBefore)

            // Q4 — 매칭 부재 row 가 잔존하면 WARN
            val unmatchedAfter = countUnmatched(spec, quotedSource, quotedRef)
            if (unmatchedAfter > 0) {
                log.warn(
                    "[fk-natural-key] {} : {} row 매칭 실패 — {} (ref={}.{})",
                    label, unmatchedAfter, spec.sourceColumn, spec.refTable, spec.refColumn,
                )
            }
        }

        log.info("[fk-natural-key] done — total {} rows updated across {} mappings", totalUpdated, NATURAL_KEY_FK_MAPPINGS.size)

        return SfMigrationStage2Response(
            substep = "fk-natural-key",
            results = results,
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * UPDATE SQL builder — 단위 테스트 검증 용으로 분리.
     *
     * 식별자 모두 컴파일 타임 상수 (NATURAL_KEY_FK_MAPPINGS) — SQL injection 없음.
     */
    internal fun buildUpdateSql(spec: NaturalKeyFkSpec): String {
        val quotedSource = quoteIdentifier(spec.sourceTable)
        val quotedRef = quoteIdentifier(spec.refTable)
        return """
            UPDATE $schemaName.$quotedSource s
            SET ${spec.targetIdColumn} = r.${spec.targetIdColumn}
            FROM $schemaName.$quotedRef r
            WHERE r.${spec.refColumn} = s.${spec.sourceColumn}
              AND s.${spec.targetIdColumn} IS NULL
        """.trimIndent()
    }

    /**
     * source table 의 target id 컬럼이 NULL 인 row 개수 (매칭 부재 측정용).
     */
    private fun countUnmatched(
        spec: NaturalKeyFkSpec,
        quotedSource: String,
        @Suppress("UNUSED_PARAMETER") quotedRef: String,
    ): Long {
        val sql = """
            SELECT COUNT(*)
            FROM $schemaName.$quotedSource s
            WHERE s.${spec.targetIdColumn} IS NULL
              AND s.${spec.sourceColumn} IS NOT NULL
        """.trimIndent()
        val result = em.createNativeQuery(sql).singleResult
        return (result as Number).toLong()
    }

    /**
     * PG reserved keyword `user` 대응. 본 service 가 처리하는 ref/source 중 `user` 는 미사용이지만
     * 향후 확장 안전성을 위해 동일 패턴 유지.
     */
    private fun quoteIdentifier(name: String): String = if (name == "user") "\"user\"" else name
}
