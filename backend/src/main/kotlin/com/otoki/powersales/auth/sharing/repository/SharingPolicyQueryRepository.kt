package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.dto.SharingRuleSnapshot
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * SharingRule 본문을 User 에게 매칭되는 evaluatorRules 형태로 한 번에 조회 (spec #782 P3-B).
 *
 * AdminDataScopeService.resolve() 가 본 Repository 를 1회 호출하여 DataScope.evaluatorRules 채움 →
 * SharingRulePolicyEvaluator 가 매 admin read 마다 sharing_rule / condition / target 테이블 별도
 * lookup 회피.
 *
 * ## 매칭 알고리즘
 *
 * 본 User 에게 적용 가능한 sharingRule = `sharing_rule_target` 의 다음 조건 중 하나 만족:
 * - `target_type = ROLE` AND `target_id = user.userRoleId`
 * - `target_type IN (ROLE_AND_SUBORDINATES, ROLE_AND_SUBORDINATES_INTERNAL)` AND
 *   `target_id IN ancestorPath(user.userRoleId)`
 * - `target_type = GROUP` AND `target_id IN groupMemberships`
 * - `target_type = USER` AND `target_id = user.userId`
 *
 * 매칭된 sharing_rule_id 의 본문 + condition list 를 SharingRuleSnapshot 으로 합성.
 */
@Repository
@Transactional(readOnly = true)
class SharingPolicyQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 본 User 에게 매칭되는 sharingRule 본문 일람.
     *
     * @param userId User PK
     * @param userRoleId User.userRoleId — null 가능 (미할당 user)
     * @param ancestorPath UserRoleHierarchyTraversal.getAncestorPath 결과
     * @param groupMemberships GroupMembershipEvaluator.getMemberGroupIds 결과
     */
    @Suppress("UNCHECKED_CAST")
    fun findRulesForUser(
        userId: Long,
        userRoleId: Long?,
        ancestorPath: List<Long>,
        groupMemberships: Set<Long>,
    ): List<SharingRuleSnapshot> {
        // target 매칭 sharing_rule_id 일람
        val ruleIdSql = """
            SELECT DISTINCT sr.sharing_rule_id, sr.developer_name, sr.s_object_name,
                            sr.rule_type, sr.access_level, sr.include_owned_by_all
              FROM powersales.sharing_rule sr
              JOIN powersales.sharing_rule_target srt ON srt.sharing_rule_id = sr.sharing_rule_id
             WHERE (
                   (srt.target_type = 'ROLE' AND srt.target_id = :userRoleId)
                OR (srt.target_type IN ('ROLE_AND_SUBORDINATES', 'ROLE_AND_SUBORDINATES_INTERNAL')
                    AND srt.target_id = ANY(CAST(:ancestorPath AS BIGINT[])))
                OR (srt.target_type = 'GROUP'
                    AND srt.target_id = ANY(CAST(:groupMemberships AS BIGINT[])))
                OR (srt.target_type = 'USER' AND srt.target_id = :userId)
             )
        """.trimIndent()

        val ruleRows = entityManager.createNativeQuery(ruleIdSql)
            .setParameter("userId", userId)
            .setParameter("userRoleId", userRoleId ?: -1L)
            .setParameter("ancestorPath", toLongArrayLiteral(ancestorPath))
            .setParameter("groupMemberships", toLongArrayLiteral(groupMemberships))
            .resultList as List<Array<Any?>>

        if (ruleRows.isEmpty()) return emptyList()

        val ruleMap: Map<Long, RuleHeader> = ruleRows.associate {
            (it[0] as Number).toLong() to RuleHeader(
                sharingRuleId = (it[0] as Number).toLong(),
                developerName = it[1] as String,
                sObjectName = it[2] as String,
                ruleType = it[3] as String,
                accessLevel = it[4] as String,
                includeOwnedByAll = it[5] as Boolean,
            )
        }

        // condition 일괄 조회
        val conditionSql = """
            SELECT sharing_rule_id, field, operator, value, condition_order, logic_connector
              FROM powersales.sharing_rule_condition
             WHERE sharing_rule_id = ANY(CAST(:ruleIds AS BIGINT[]))
             ORDER BY sharing_rule_id, condition_order
        """.trimIndent()

        val conditionRows = entityManager.createNativeQuery(conditionSql)
            .setParameter("ruleIds", toLongArrayLiteral(ruleMap.keys))
            .resultList as List<Array<Any?>>

        val conditionsByRule = conditionRows.groupBy(
            { (it[0] as Number).toLong() },
            {
                SharingRuleSnapshot.ConditionSnapshot(
                    field = it[1] as String,
                    operator = it[2] as String,
                    value = it[3] as String?,
                    conditionOrder = (it[4] as Number).toInt(),
                    logicConnector = it[5] as String?,
                )
            },
        )

        return ruleMap.values.map { h ->
            SharingRuleSnapshot(
                sharingRuleId = h.sharingRuleId,
                developerName = h.developerName,
                sObjectName = h.sObjectName,
                ruleType = h.ruleType,
                accessLevel = h.accessLevel,
                includeOwnedByAll = h.includeOwnedByAll,
                conditions = conditionsByRule[h.sharingRuleId] ?: emptyList(),
            )
        }
    }

    private fun toLongArrayLiteral(values: Collection<Long>): String {
        if (values.isEmpty()) return "{}"
        return values.joinToString(prefix = "{", postfix = "}")
    }

    private data class RuleHeader(
        val sharingRuleId: Long,
        val developerName: String,
        val sObjectName: String,
        val ruleType: String,
        val accessLevel: String,
        val includeOwnedByAll: Boolean,
    )
}
