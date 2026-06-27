package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.dto.SharingRuleSnapshot
import com.otoki.powersales.platform.auth.sharing.entity.QSharingRule.Companion.sharingRule
import com.otoki.powersales.platform.auth.sharing.entity.QSharingRuleCondition.Companion.sharingRuleCondition
import com.otoki.powersales.platform.auth.sharing.entity.QSharingRuleTarget.Companion.sharingRuleTarget
import com.otoki.powersales.platform.common.config.CacheConfig
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.cache.annotation.Cacheable
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
 *
 * ## QueryDSL 사용 — 컴파일 타임 검증
 * 이전 구현은 native SQL 이었으나 `value` (entity field) ↔ `condition_value` (DB 컬럼) 매핑 오류가
 * 컴파일에서 잡히지 않아 잠복했다. QueryDSL 로 전환하여 entity property 기반 type-safe 쿼리 사용 —
 * 컬럼명 변경 시 컴파일 깨짐으로 즉시 발견.
 *
 * ## sfid 직접 매칭 금지 정책 정합
 * audit/owner field (CreatedById / LastModifiedById / OwnerId) condition 의 `condition_value` 는
 * SF user sfid 다. 본 Repository 는 런타임에 sfid 를 일절 매칭하지 않는다 — Stage2 FK Resolve 가
 * 적재 시점에 `condition_resolved_user_id` (신규 User.id) 를 미리 채워두므로, 그 FK 컬럼을 그대로
 * SELECT 해 [SharingRuleSnapshot.ConditionSnapshot.resolvedUserId] 로 전달한다. evaluator 는 그
 * Long 값과 entity FK relation (`createdBy.id` / `ownerUser.id`) 을 비교한다.
 */
@Repository
@Transactional(readOnly = true)
class SharingPolicyQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /**
     * 본 User 에게 매칭되는 sharingRule 본문 일람.
     *
     * @param userId User PK
     * @param userRoleId User.userRoleId — null 가능 (미할당 user)
     * @param ancestorPath UserRoleHierarchyTraversal.getAncestorPath 결과
     * @param groupMemberships GroupMembershipEvaluator.getMemberGroupIds 결과
     *
     * ## 캐시 정합
     *
     * `#userId` 단일 키. 입력 4종 중 `ancestorPath` / `groupMemberships` 는 본 호출처
     * ([com.otoki.powersales.admin.service.AdminDataScopeService.enrichWithSharingPolicy]) 에서
     * `UserRoleHierarchyTraversal` / `GroupMembershipEvaluator` 의 1h TTL 캐시 산출물을 그대로 전달 —
     * 두 산출물이 변경되는 운영 사건 (UserRole 계층 변경, Group 변경, sharing_rule CUD) 은 모두
     * [com.otoki.powersales.platform.auth.sharing.service.SharingRecalcService.recalcAll] 로 일괄 evict.
     * 본 cache name 도 [CacheConfig.SHARING_RELATED_CACHE_NAMES] 에 등재되어 함께 무효화.
     *
     * `userRoleId` 가 null 인 user (미할당) 와 non-null user 는 동일 userId 키지만 user.userRoleId
     * 자체가 user 갱신 사건 — sharing recalc 시점에 evict 되는 정합 범위.
     */
    @Cacheable(value = [CacheConfig.CACHE_SHARING_RULES_FOR_USER], key = "#userId")
    fun findRulesForUser(
        userId: Long,
        userRoleId: Long?,
        ancestorPath: List<Long>,
        groupMemberships: Set<Long>,
    ): List<SharingRuleSnapshot> {
        val targetPredicate = buildTargetPredicate(
            userId = userId,
            userRoleId = userRoleId,
            ancestorPath = ancestorPath,
            groupMemberships = groupMemberships,
        ) ?: return emptyList()

        // 1) target 매칭 sharing_rule 본문 일람 (DISTINCT)
        val ruleRows = queryFactory
            .selectDistinct(
                sharingRule.id,
                sharingRule.developerName,
                sharingRule.sObjectName,
                sharingRule.ruleType,
                sharingRule.accessLevel,
                sharingRule.includeOwnedByAll,
            )
            .from(sharingRule)
            .join(sharingRuleTarget)
            .on(sharingRuleTarget.sharingRuleId.eq(sharingRule.id))
            .where(targetPredicate)
            .fetch()

        if (ruleRows.isEmpty()) return emptyList()

        val ruleMap: Map<Long, RuleHeader> = ruleRows.associate { tuple ->
            val id = tuple.get(sharingRule.id)!!
            id to RuleHeader(
                sharingRuleId = id,
                developerName = tuple.get(sharingRule.developerName)!!,
                sObjectName = tuple.get(sharingRule.sObjectName)!!,
                ruleType = tuple.get(sharingRule.ruleType)!!,
                accessLevel = tuple.get(sharingRule.accessLevel)!!,
                includeOwnedByAll = tuple.get(sharingRule.includeOwnedByAll)!!,
            )
        }

        // 2) condition 일괄 조회 — entity property `value` 가 DB 컬럼 `condition_value` 로 자동 매핑.
        // audit/owner field (CreatedById/LastModifiedById/OwnerId) 의 신규 User.id 는
        // Stage2 FK Resolve 가 미리 채운 `condition_resolved_user_id` 컬럼에서 그대로 읽는다 —
        // 런타임 sfid 매칭 없음 (sfid 직접 매칭 금지 정책).
        val conditionRows = queryFactory
            .select(
                sharingRuleCondition.sharingRuleId,
                sharingRuleCondition.field,
                sharingRuleCondition.operator,
                sharingRuleCondition.value,
                sharingRuleCondition.conditionOrder,
                sharingRuleCondition.logicConnector,
                sharingRuleCondition.resolvedUserId,
            )
            .from(sharingRuleCondition)
            .where(sharingRuleCondition.sharingRuleId.`in`(ruleMap.keys))
            .orderBy(
                sharingRuleCondition.sharingRuleId.asc(),
                sharingRuleCondition.conditionOrder.asc(),
            )
            .fetch()

        val conditionsByRule = conditionRows.groupBy(
            { it.get(sharingRuleCondition.sharingRuleId)!! },
            {
                SharingRuleSnapshot.ConditionSnapshot(
                    field = it.get(sharingRuleCondition.field)!!,
                    operator = it.get(sharingRuleCondition.operator)!!,
                    value = it.get(sharingRuleCondition.value),
                    conditionOrder = it.get(sharingRuleCondition.conditionOrder)!!,
                    logicConnector = it.get(sharingRuleCondition.logicConnector),
                    resolvedUserId = it.get(sharingRuleCondition.resolvedUserId),
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

    /**
     * sharing_rule_target 매칭 4 branch 의 OR 조건 합성.
     *
     * 입력이 모두 비어 있으면 null 반환 → 호출처에서 즉시 empty 반환 (불필요한 query 회피).
     */
    private fun buildTargetPredicate(
        userId: Long,
        userRoleId: Long?,
        ancestorPath: List<Long>,
        groupMemberships: Set<Long>,
    ): BooleanExpression? {
        val branches = mutableListOf<BooleanExpression>()

        // 1) target_type = ROLE AND target_id = userRoleId
        if (userRoleId != null) {
            branches += sharingRuleTarget.targetType.eq(TARGET_TYPE_ROLE)
                .and(sharingRuleTarget.targetId.eq(userRoleId))
        }
        // 2) target_type IN (ROLE_AND_SUBORDINATES, *_INTERNAL) AND target_id IN ancestorPath
        if (ancestorPath.isNotEmpty()) {
            branches += sharingRuleTarget.targetType.`in`(TARGET_TYPES_ROLE_AND_SUBORDINATES)
                .and(sharingRuleTarget.targetId.`in`(ancestorPath))
        }
        // 3) target_type = GROUP AND target_id IN groupMemberships
        if (groupMemberships.isNotEmpty()) {
            branches += sharingRuleTarget.targetType.eq(TARGET_TYPE_GROUP)
                .and(sharingRuleTarget.targetId.`in`(groupMemberships))
        }
        // 4) target_type = USER AND target_id = userId
        branches += sharingRuleTarget.targetType.eq(TARGET_TYPE_USER)
            .and(sharingRuleTarget.targetId.eq(userId))

        return branches.reduce { acc, b -> acc.or(b) }
    }

    private data class RuleHeader(
        val sharingRuleId: Long,
        val developerName: String,
        val sObjectName: String,
        val ruleType: String,
        val accessLevel: String,
        val includeOwnedByAll: Boolean,
    )

    companion object {
        private const val TARGET_TYPE_ROLE = "ROLE"
        private const val TARGET_TYPE_GROUP = "GROUP"
        private const val TARGET_TYPE_USER = "USER"
        private val TARGET_TYPES_ROLE_AND_SUBORDINATES: List<String> = listOf(
            "ROLE_AND_SUBORDINATES",
            "ROLE_AND_SUBORDINATES_INTERNAL",
        )
    }
}
