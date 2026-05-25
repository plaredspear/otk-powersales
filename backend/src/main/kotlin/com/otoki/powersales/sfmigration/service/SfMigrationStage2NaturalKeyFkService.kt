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

        // sharing_rule_condition / sharing_rule_target 의 sharing_rule_id resolve —
        // (s_object_name, developer_name) 복합 자연 키 필요 (단일 NaturalKeyFkSpec 으로 표현 불가).
        // SF retrieve 의 같은 developerName SharingRule 이 여러 sObject 의 sharingRules-meta.xml
        // 에 정의될 수 있으므로 developer_name 단일 키 매칭 시 unique constraint
        // idx_sharing_rule_condition_rule_order_unique 위반.
        totalUpdated += resolveSharingRuleSubtableFk(results)

        // sharing_rule_target — polymorphic target_id resolve.
        // 단일 NaturalKeyFkSpec 으로 표현 불가 (target_type 별 ref table 분기) — 전용 method 처리.
        totalUpdated += resolveSharingRuleTarget(results)

        // permission_set_flags.permission_set_sfid 채움 — source/ref 컬럼명이 비대칭
        // (source = permission_set_sfid, ref = sfid) 라 단일 NaturalKeyFkSpec 으로 표현 불가.
        // permission_set 의 PSA fk substep (permission_set_assignment.permission_set_sfid → permission_set_flags_id)
        // 직전에 실행되어야 하므로 본 메서드가 NATURAL_KEY_FK_MAPPINGS 일괄 적용 직후 호출.
        totalUpdated += resolvePermissionSetFlagsSfid(results)

        log.info("[fk-natural-key] done — total {} rows updated", totalUpdated)

        return SfMigrationStage2Response(
            substep = "fk-natural-key",
            results = results,
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * sharing_rule_condition / sharing_rule_target 의 sharing_rule_id 채움 —
     * (s_object_name, developer_name) 복합 자연 키로 sharing_rule lookup.
     *
     * 한 retrieve 산출물 안에서 같은 developerName 의 SharingRule 이 여러 sObject 의
     * sharingRules-meta.xml 에 동시 정의될 수 있다 (예: X5452 가 Account /
     * DisplayWorkScheduleMaster__c / MonthlyFemaleEmployeeIntegrationSchedule__c
     * 3개 sObject 의 sharingRules 에 정의 → sharing_rule 3 row 생성).
     *
     * 단일 컬럼 (developer_name) 매칭 시 첫 번째 sharing_rule.sharing_rule_id 가 모든
     * 동명 condition row 에 채워져 (sharing_rule_id, condition_order) unique index
     * 위반 발생. 두 컬럼 (s_object_name, developer_name) 복합 키 매칭으로 해소.
     */
    private fun resolveSharingRuleSubtableFk(results: MutableList<SubstepResult>): Int {
        var totalUpdated = 0

        val conditionSql = """
            UPDATE powersales.sharing_rule_condition s
            SET sharing_rule_id = r.sharing_rule_id
            FROM powersales.sharing_rule r
            WHERE r.s_object_name = s.sharing_rule_s_object_name
              AND r.developer_name = s.sharing_rule_developer_name
              AND s.sharing_rule_id IS NULL
        """.trimIndent()
        val conditionUpdated = em.createNativeQuery(conditionSql).executeUpdate()
        results += SubstepResult(
            label = "sharing_rule_condition.(s_object_name, developer_name) → sharing_rule.sharing_rule_id",
            rowsAffected = conditionUpdated,
        )
        totalUpdated += conditionUpdated
        log.info("[fk-natural-key] sharing_rule_condition.sharing_rule_id : updated={}", conditionUpdated)

        val targetSql = """
            UPDATE powersales.sharing_rule_target s
            SET sharing_rule_id = r.sharing_rule_id
            FROM powersales.sharing_rule r
            WHERE r.s_object_name = s.sharing_rule_s_object_name
              AND r.developer_name = s.sharing_rule_developer_name
              AND s.sharing_rule_id IS NULL
        """.trimIndent()
        val targetUpdated = em.createNativeQuery(targetSql).executeUpdate()
        results += SubstepResult(
            label = "sharing_rule_target.(s_object_name, developer_name) → sharing_rule.sharing_rule_id",
            rowsAffected = targetUpdated,
        )
        totalUpdated += targetUpdated
        log.info("[fk-natural-key] sharing_rule_target.sharing_rule_id : updated={}", targetUpdated)

        val unmatchedConditionSql = """
            SELECT COUNT(*) FROM powersales.sharing_rule_condition
            WHERE sharing_rule_id IS NULL AND sharing_rule_developer_name IS NOT NULL
        """.trimIndent()
        val unmatchedCondition = (em.createNativeQuery(unmatchedConditionSql).singleResult as Number).toLong()
        if (unmatchedCondition > 0) {
            log.warn(
                "[fk-natural-key] sharing_rule_condition : {} row 매칭 실패 — (s_object_name, developer_name) 매칭 안 됨",
                unmatchedCondition,
            )
        }

        val unmatchedTargetSql = """
            SELECT COUNT(*) FROM powersales.sharing_rule_target
            WHERE sharing_rule_id IS NULL AND sharing_rule_developer_name IS NOT NULL
        """.trimIndent()
        val unmatchedTarget = (em.createNativeQuery(unmatchedTargetSql).singleResult as Number).toLong()
        if (unmatchedTarget > 0) {
            log.warn(
                "[fk-natural-key] sharing_rule_target : {} row 매칭 실패 — (s_object_name, developer_name) 매칭 안 됨",
                unmatchedTarget,
            )
        }

        return totalUpdated
    }

    /**
     * sharing_rule_target.target_id 채움 — target_type 별 ref table 분기.
     *
     * SF retrieve XML 의 `<sharedTo>` 본문 element 가 DeveloperName 만 보존하므로
     * target_sfid 는 채울 출처 없음 (영구 NULL). target_developer_name + target_type
     * 자연 키만으로 target_id resolve.
     *
     * target_type 매칭:
     *   - 'ROLE' / 'ROLE_AND_SUBORDINATES' / 'ROLE_AND_SUBORDINATES_INTERNAL'
     *     → user_role.developer_name lookup → user_role_id
     *   - 'GROUP' → "group".developer_name lookup → group_id
     *   - 'USER'  → user.employee_code (혹은 SF 매핑 자연 키) — SF retrieve 의 `<user>` 본문
     *              은 username 형식이라 신규 시스템 employee_code 와 정합 불일치 → 운영 0건
     *              대응이라 본 분기는 noop, 향후 사용 시 매핑 키 결정 필요.
     */
    private fun resolveSharingRuleTarget(results: MutableList<SubstepResult>): Int {
        var totalUpdated = 0

        // ROLE / ROLE_AND_SUBORDINATES / ROLE_AND_SUBORDINATES_INTERNAL → user_role.user_role_id
        val roleSql = """
            UPDATE powersales.sharing_rule_target s
            SET target_id = r.user_role_id
            FROM powersales.user_role r
            WHERE r.developer_name = s.target_developer_name
              AND s.target_type IN ('ROLE', 'ROLE_AND_SUBORDINATES', 'ROLE_AND_SUBORDINATES_INTERNAL')
              AND s.target_id IS NULL
        """.trimIndent()
        val roleUpdated = em.createNativeQuery(roleSql).executeUpdate()
        results += SubstepResult(
            label = "sharing_rule_target.target_developer_name (target_type=ROLE*) → user_role.user_role_id",
            rowsAffected = roleUpdated,
        )
        totalUpdated += roleUpdated
        log.info("[fk-natural-key] sharing_rule_target target_type=ROLE* : updated={}", roleUpdated)

        // GROUP → "group".group_id
        val groupSql = """
            UPDATE powersales.sharing_rule_target s
            SET target_id = r.group_id
            FROM powersales."group" r
            WHERE r.developer_name = s.target_developer_name
              AND s.target_type = 'GROUP'
              AND s.target_id IS NULL
        """.trimIndent()
        val groupUpdated = em.createNativeQuery(groupSql).executeUpdate()
        results += SubstepResult(
            label = "sharing_rule_target.target_developer_name (target_type=GROUP) → group.group_id",
            rowsAffected = groupUpdated,
        )
        totalUpdated += groupUpdated
        log.info("[fk-natural-key] sharing_rule_target target_type=GROUP : updated={}", groupUpdated)

        val unmatchedSql = """
            SELECT COUNT(*) FROM powersales.sharing_rule_target
            WHERE target_id IS NULL AND target_developer_name IS NOT NULL
        """.trimIndent()
        val unmatched = (em.createNativeQuery(unmatchedSql).singleResult as Number).toLong()
        if (unmatched > 0) {
            log.warn("[fk-natural-key] sharing_rule_target : {} row 매칭 실패 (target_developer_name 매칭 안 됨)", unmatched)
        }

        return totalUpdated
    }

    /**
     * permission_set_flags.permission_set_sfid 채움 — XML 메타 출처라 Stage1 시점 NULL.
     *
     * permission_set_flags.permission_set_name → permission_set.name lookup 후
     * permission_set.sfid 를 permission_set_flags.permission_set_sfid 에 채움.
     *
     * 후속 PSA fk substep (permission_set_assignment.permission_set_sfid → permission_set_flags_id)
     * 이 본 메서드 결과에 의존.
     */
    private fun resolvePermissionSetFlagsSfid(results: MutableList<SubstepResult>): Int {
        val sql = """
            UPDATE powersales.permission_set_flags s
            SET permission_set_sfid = r.sfid
            FROM powersales.permission_set r
            WHERE r.name = s.permission_set_name
              AND s.permission_set_sfid IS NULL
        """.trimIndent()
        val updated = em.createNativeQuery(sql).executeUpdate()
        results += SubstepResult(
            label = "permission_set_flags.permission_set_name → permission_set.sfid",
            rowsAffected = updated,
        )
        log.info("[fk-natural-key] permission_set_flags.permission_set_sfid : updated={}", updated)

        val unmatchedSql = """
            SELECT COUNT(*) FROM powersales.permission_set_flags
            WHERE permission_set_sfid IS NULL AND permission_set_name IS NOT NULL
        """.trimIndent()
        val unmatched = (em.createNativeQuery(unmatchedSql).singleResult as Number).toLong()
        if (unmatched > 0) {
            log.warn(
                "[fk-natural-key] permission_set_flags : {} row 매칭 실패 (permission_set_name 매칭 안 됨)",
                unmatched,
            )
        }

        return updated
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
     * PG reserved keyword `user` / `group` 대응.
     */
    private fun quoteIdentifier(name: String): String =
        if (name == "user" || name == "group") "\"$name\"" else name
}
