package com.otoki.powersales.auth.sharing.dto

/**
 * DataScope.evaluatorRules 의 immutable item (spec #782 P3-B).
 *
 * SharingPolicyQueryRepository 가 본 User 에게 매칭되는 sharingRule 본문을 한 번에 조회하여
 * 본 DTO 의 list 로 채움. SharingRulePolicyEvaluator 가 매 request 마다 sharing_rule / condition /
 * target 테이블 별도 lookup 회피 — populating 1회 + Predicate 빌드 다회.
 */
data class SharingRuleSnapshot(
    val sharingRuleId: Long,
    val developerName: String,
    val sObjectName: String,
    val ruleType: String, // CRITERIA / OWNER
    val accessLevel: String, // Read / Edit
    val includeOwnedByAll: Boolean,
    val conditions: List<ConditionSnapshot>,
) {
    data class ConditionSnapshot(
        val field: String,
        val operator: String,
        val value: String?,
        val conditionOrder: Int,
        val logicConnector: String?, // AND / OR / null
    )
}
