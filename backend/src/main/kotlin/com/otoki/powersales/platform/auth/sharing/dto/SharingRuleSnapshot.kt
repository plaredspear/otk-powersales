package com.otoki.powersales.platform.auth.sharing.dto

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
        /**
         * audit/owner field (`CreatedById` / `LastModifiedById` / `OwnerId`) 의 value 가 SF user sfid (18자) 인 경우,
         * **Stage2 FK Resolve 적재 시점**에 user.sfid lookup 으로 미리 변환해 둔 신규 시스템 User.id (Long).
         * `sharing_rule_condition.condition_resolved_user_id` 컬럼을 그대로 읽어온 값이다.
         *
         * application 로직은 신규 시스템 PK 만 사용하는 정책 (sfid 직접 매칭 금지) — evaluator 는 본 값을
         * FK relation (`createdBy.id` / `ownerUser.id`) 와 Long 비교한다. 비-audit field 또는 sfid 매칭 실패 시
         * null — evaluator 가 해당 condition 을 skip 한다. 런타임에는 sfid 매칭을 일절 수행하지 않는다.
         */
        val resolvedUserId: Long? = null,
    )
}
