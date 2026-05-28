package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.dto.SharingRuleSnapshot
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.EntityPathBase
import com.querydsl.core.types.dsl.Expressions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * SF Sharing Rule 정책 evaluator — DataScope 입력으로 QueryDSL Predicate 합성 (spec #782 P3-B).
 *
 * Repository 의 admin read query 에 본 결과 Predicate 를 `where(...)` 절로 추가.
 *
 * ## 우선순위 평가 (높음 → 낮음)
 * 1. **권한 매트릭스 최우선 분기** — Profile.viewAllData / PermissionSet.viewAllData /
 *    PermissionSet.viewAllRecords[SObject] true 면 no-filter return (전체 row 가시).
 * 2-5. **OR 합성** — Owner / Hierarchy / SharingRule / Legacy branchCodes 중 하나라도 매칭이면 가시.
 * 6. **ControlledByParent** — 자식 SObject 의 read query 가 부모 SObject 의 가시성 흡수.
 *
 * ## EntityPath 접근
 * `Expressions.stringPath(entityPath, "field_name")` 패턴으로 runtime field access — SObject 별
 * 정적 Q-class 분기 회피. 단 `field_name` 은 entity 의 DB column name 이 아닌 JPA property name.
 * SharingRule meta 의 `<field>` 는 SF API name (예: `CostCenterCode__c`) — 본 evaluator 가
 * SF API name → JPA property 매핑 처리.
 */
@Service
class SharingRulePolicyEvaluator(
    private val sObjectSettingProvider: SObjectSettingProvider,
    private val recordTypePermissionEvaluator: RecordTypePermissionEvaluator? = null,
) {

    private val log = LoggerFactory.getLogger(SharingRulePolicyEvaluator::class.java)

    /**
     * 본 SObject 의 read query 에 합성할 Predicate.
     *
     * @param scope AdminDataScopeService.resolve(principal) 결과
     * @param sObjectName SF SObject API name (예: `Account`, `DKRetail__Promotion__c`)
     * @param entityPath QueryDSL Q-class (예: `QAccount.account`, `QPromotion.promotion`)
     */
    fun buildPredicate(
        scope: DataScope,
        sObjectName: String,
        entityPath: EntityPathBase<*>,
    ): Predicate {
        // 우선순위 1 — viewAllData / viewAllRecords[SObject]
        if (hasUnrestrictedAccess(scope, sObjectName)) {
            log.debug("[sharing-policy] {} userId={} — unrestricted access", sObjectName, scope.userId)
            return Expressions.asBoolean(true).isTrue
        }

        // 우선순위 1b — OWD 평가 (spec #791)
        // PublicReadWrite / ReadWrite — 전체 row 가시 (조건 평가 생략)
        // PublicReadOnly / Read — read 무조건 가시 (본 evaluator 는 read context — 통과)
        // Private / ControlledByParent / fallback — 아래 OR 합성으로 진행
        val owd = sObjectSettingProvider.orgWideDefault(sObjectName)
        if (owd == SObjectSettingProvider.OWD_PUBLIC_READ_WRITE ||
            owd == SObjectSettingProvider.OWD_READ_WRITE ||
            owd == SObjectSettingProvider.OWD_PUBLIC_READ_ONLY ||
            owd == SObjectSettingProvider.OWD_READ
        ) {
            log.debug("[sharing-policy] {} userId={} owd={} — read pass", sObjectName, scope.userId, owd)
            return Expressions.asBoolean(true).isTrue
        }

        val predicates = mutableListOf<BooleanExpression>()

        // 2 — Owner
        ownerPredicate(scope, entityPath)?.let { predicates += it }

        // 3 — UserRole Hierarchy (record.owner.user_role_id IN allSubordinateUserRoleIds)
        // OWD 의 hierarchy 옵트인 (spec #791) 이 true 일 때만 평가
        if (sObjectSettingProvider.allowHierarchyGrant(sObjectName)) {
            hierarchyPredicate(scope, entityPath)?.let { predicates += it }
        }

        // 4 — SharingRule 본문
        sharingRulePredicate(scope, sObjectName, entityPath)?.let { predicates += it }

        // 5 — Legacy branchCodes (기존 추상화)
        legacyBranchPredicate(scope, entityPath)?.let { predicates += it }

        // 6 — ControlledByParent — 부모 SObject 의 predicate join
        // 자식 SObject 의 cross-entity join 합성은 Repository 호출 측이 직접 처리 (entityPath 가 부모 alias 지정).
        // 본 evaluator 는 자식 read query 가 parent entityPath 전달 시 동일 로직으로 동작.

        if (predicates.isEmpty()) {
            log.debug("[sharing-policy] {} userId={} — deny (no matching predicate)", sObjectName, scope.userId)
            return Expressions.asBoolean(false).isTrue
        }

        // OR 합성
        val mainPredicate = predicates.reduce { acc, expr -> acc.or(expr) }

        // 7 — Record Type 권한 분기 (spec #794)
        // entity 의 record_type_id 컬럼이 있는 경우만 AND 합성.
        // Q1 옵션 1: 컬럼 부재 시 자동 skip (catch 분기로 skip)
        // Q2 옵션 1: visibleRecordTypeIds 가 빈 set 이면 sObject 의 record_type_id IS NOT NULL row 차단
        val rtPredicate = recordTypeAndPredicate(scope, entityPath)
        return if (rtPredicate != null) mainPredicate.and(rtPredicate) else mainPredicate
    }

    /**
     * record_type_id 분기 — Q1 옵션 1 (NULL OR 분기) + Q2 옵션 1 (가시 RT 0건 시 NOT NULL row 차단).
     *
     * entity 가 record_type_id 컬럼을 보유하지 않으면 null return (catch 분기 — skip).
     */
    internal fun recordTypeAndPredicate(
        scope: DataScope,
        entityPath: EntityPathBase<*>,
    ): BooleanExpression? {
        if (!hasProperty(entityPath, "recordTypeId")) {
            log.debug("[sharing-policy] entity {} has no recordTypeId — skip RT predicate", entityPath)
            return null
        }
        return try {
            val path = Expressions.numberPath(java.lang.Long::class.java, entityPath, "recordTypeId")
            if (scope.visibleRecordTypeIds.isEmpty()) {
                // Q2 옵션 1: 가시 RT 0건 — record_type_id IS NOT NULL row 차단
                // 즉 record_type_id IS NULL row 만 통과 (Q1 옵션 1 동등)
                path.isNull
            } else {
                // record_type_id IS NULL OR record_type_id IN visibleRecordTypeIds
                val values: List<java.lang.Long> = scope.visibleRecordTypeIds.map { it as java.lang.Long }
                path.isNull.or(path.`in`(values))
            }
        } catch (e: Exception) {
            log.debug("[sharing-policy] entity {} has no recordTypeId — skip RT predicate", entityPath, e)
            null
        }
    }

    /**
     * 우선순위 1 — Profile.viewAllData 또는 PermissionSet.viewAllRecords[SObject] true 여부.
     */
    fun hasUnrestrictedAccess(scope: DataScope, sObjectName: String): Boolean {
        if (scope.profileFlags.viewAllData) return true
        if (scope.permissionSetFlags.hasViewAllRecords(sObjectName)) return true
        return false
    }

    /**
     * 우선순위 2 — Owner 매칭 (record.owner_id = userId).
     *
     * entityPath 의 `ownerId` Long? property 가 존재한다고 가정. 부재 시 null return (predicate 합성 생략).
     */
    fun ownerPredicate(scope: DataScope, entityPath: EntityPathBase<*>): BooleanExpression? {
        val userId = scope.userId ?: return null
        // QueryDSL Path 생성 자체는 entity field 존재 검증을 하지 않음 (실패 시 Hibernate SQL
        // 컴파일 시점에 UnknownPathException). reflection 으로 Q-class field 사전 확인.
        // 운영 표준 owner 명명: `ownerUser` (User? relation, FK = owner_user_id). 단순 `ownerId` 는
        // 운영 entity 에서 사용 사례 0건이지만 backward compat 차 fallback 유지.
        // 운영 표준 owner 명명 = `ownerUser` (User? relation, FK owner_user_id). 단순 `ownerId` 단순
        // 필드는 운영 entity 0건 — fallback 만 유지.
        val ownerProperty = when {
            hasProperty(entityPath, "ownerUser") -> "ownerUser.id"
            hasProperty(entityPath, "ownerId") -> "ownerId"
            else -> {
                log.debug("[sharing-policy] entity {} has no owner property — skip owner predicate", entityPath)
                return null
            }
        }
        return try {
            @Suppress("UNCHECKED_CAST")
            val ownerIdPath = Expressions.numberPath(java.lang.Long::class.java, entityPath, ownerProperty)
            ownerIdPath.eq(userId as java.lang.Long)
        } catch (e: Exception) {
            log.debug("[sharing-policy] entity {} owner predicate build failed ({})", entityPath, ownerProperty, e)
            null
        }
    }

    /**
     * 우선순위 3 — UserRole Hierarchy (record.owner.user_role_id IN allSubordinateUserRoleIds).
     *
     * entity 의 `owner` relation 의 `userRoleId` property 활용. 부재 시 null.
     * scope.allSubordinateUserRoleIds 가 비어있으면 predicate 합성 생략.
     */
    fun hierarchyPredicate(scope: DataScope, entityPath: EntityPathBase<*>): BooleanExpression? {
        if (scope.allSubordinateUserRoleIds.isEmpty()) return null
        // entity 의 owner relation 의 `userRoleId` property 활용. 운영 표준 명명 = `ownerUser`.
        // 운영 표준 owner 명명 = `ownerUser` (User?). `owner` 단순 명명은 운영 0건 — fallback 만 유지.
        val ownerRelation = when {
            hasProperty(entityPath, "ownerUser") -> "ownerUser.userRoleId"
            hasProperty(entityPath, "owner") -> "owner.userRoleId"
            else -> {
                log.debug("[sharing-policy] entity {} has no owner relation — skip hierarchy predicate", entityPath)
                return null
            }
        }
        return try {
            val path = Expressions.numberPath(java.lang.Long::class.java, entityPath, ownerRelation)
            // QueryDSL `.in(Collection<out Long>)` overload 정합 — Set<Long> 을 List<java.lang.Long> 으로 박싱.
            val values: List<java.lang.Long> = scope.allSubordinateUserRoleIds.map { it as java.lang.Long }
            path.`in`(values)
        } catch (e: Exception) {
            log.debug("[sharing-policy] entity {} hierarchy predicate build failed ({})", entityPath, ownerRelation, e)
            null
        }
    }

    /**
     * 우선순위 4 — SharingRule 본문의 condition 평가.
     *
     * scope.evaluatorRules 중 본 SObject 매칭 rule 만 필터 → 각 rule 의 condition 을 QueryDSL Predicate 변환 →
     * AND/OR logicConnector 로 합성 → 모든 rule 을 OR 로 합산.
     */
    fun sharingRulePredicate(
        scope: DataScope,
        sObjectName: String,
        entityPath: EntityPathBase<*>,
    ): BooleanExpression? {
        val rules = scope.evaluatorRules.filter { it.sObjectName == sObjectName }
        if (rules.isEmpty()) return null

        val rulePredicates = rules.mapNotNull { rule ->
            when (rule.ruleType) {
                "CRITERIA" -> buildCriteriaRulePredicate(rule, entityPath)
                "OWNER" -> {
                    // 본 프로젝트 운영 0건 — 모든 sharingRule 본문이 CRITERIA. 안전 default: 매칭 없음.
                    log.debug("[sharing-policy] OWNER rule {} skipped (0 운영 사례)", rule.developerName)
                    null
                }
                else -> {
                    log.warn("[sharing-policy] unknown ruleType {} for {}", rule.ruleType, rule.developerName)
                    null
                }
            }
        }
        if (rulePredicates.isEmpty()) return null
        return rulePredicates.reduce { acc, expr -> acc.or(expr) }
    }

    /**
     * 우선순위 5 — Legacy branchCodes (기존 #759~#780 추상화 그대로).
     *
     * scope.isAllBranches true 면 전체 가시 — 본 메서드는 호출 안 됨 (우선순위 1 에서 흡수).
     * scope.branchCodes 가 비어있으면 predicate 생략.
     */
    fun legacyBranchPredicate(scope: DataScope, entityPath: EntityPathBase<*>): BooleanExpression? {
        if (scope.isAllBranches) return Expressions.asBoolean(true).isTrue
        if (scope.branchCodes.isEmpty()) return null
        if (!hasProperty(entityPath, "costCenterCode")) {
            log.debug("[sharing-policy] entity {} has no costCenterCode — skip legacy branch predicate", entityPath)
            return null
        }
        return try {
            Expressions.stringPath(entityPath, "costCenterCode").`in`(scope.branchCodes)
        } catch (e: Exception) {
            log.debug("[sharing-policy] entity {} has no costCenterCode — skip legacy branch predicate", entityPath, e)
            null
        }
    }

    /**
     * SF API name → 실제 entity 에 평가 가능한 JPA property 경로 해석.
     *
     * **audit/owner field 는 본 메서드의 대상 아님** — `buildConditionPredicate` 가 분기로 직접 처리한다.
     * 사유: condition value 가 SF user sfid (18자 String) 인데 application 정책상 sfid 직접 매칭 금지 →
     * snapshot 적재 시점 pre-resolve 한 [SharingRuleSnapshot.ConditionSnapshot.resolvedUserId] 를 FK relation
     * (`createdBy.id` / `ownerUser.id`) 의 Long 비교에 사용.
     *
     * 일반 field 는 sfApiNameToJpaProperty 변환 후 단순 property 확인.
     */
    internal fun resolveConditionProperty(sfField: String, entityPath: EntityPathBase<*>): String? {
        val candidate = sfApiNameToJpaProperty(sfField)
        return candidate.takeIf { hasProperty(entityPath, it) }
    }

    /**
     * audit/owner field 의 FK relation 경로. entity 에 부재하면 null (condition skip).
     * - CreatedById → `createdBy.id`
     * - LastModifiedById → `lastModifiedBy.id`
     * - OwnerId → `ownerUser.id`
     */
    internal fun resolveAuditOwnerRelationPath(sfField: String, entityPath: EntityPathBase<*>): String? {
        val relation = when (sfField) {
            "CreatedById" -> "createdBy"
            "LastModifiedById" -> "lastModifiedBy"
            "OwnerId" -> "ownerUser"
            else -> return null
        }
        return if (hasProperty(entityPath, relation)) "$relation.id" else null
    }

    /**
     * QueryDSL Q-class entity path 에 [propertyName] 이 존재하는지 reflection 으로 사전 확인.
     *
     * QueryDSL `Expressions.numberPath/stringPath(entityPath, name)` 은 path object 생성만 하고
     * entity field 존재 검증을 하지 않음. SQL 컴파일 시점에 Hibernate 가 UnknownPathException 발생 —
     * try/catch 가 잡지 못함. 본 helper 가 사전 검증으로 catch fallback 을 효과 있게 만듦.
     *
     * Q-class field 는 public field (예: `branchCode: StringPath`) 또는 Kotlin lazy delegated
     * property (예: `ownerUser: QUser by lazy`) 두 형태. lazy 는 backing field 명이
     * `<name>$delegate` + getter `get<Name>()` — getter 존재 여부로 검증.
     */
    internal fun hasProperty(entityPath: EntityPathBase<*>, propertyName: String): Boolean {
        val cls = entityPath.javaClass
        // 1) public field 직접 일치
        try {
            cls.getField(propertyName)
            return true
        } catch (_: NoSuchFieldException) {
            // pass — getter 로 재시도
        }
        // 2) Kotlin lazy delegated property — getter 방식
        val getterName = "get" + propertyName.replaceFirstChar { it.uppercase() }
        return try {
            cls.getMethod(getterName)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }

    /**
     * CRITERIA rule 의 condition list → AND/OR 합성 Predicate.
     */
    internal fun buildCriteriaRulePredicate(
        rule: SharingRuleSnapshot,
        entityPath: EntityPathBase<*>,
    ): BooleanExpression? {
        if (rule.conditions.isEmpty()) return null
        val conditionPredicates = rule.conditions.sortedBy { it.conditionOrder }.mapNotNull { cond ->
            buildConditionPredicate(cond, entityPath)
        }
        if (conditionPredicates.isEmpty()) return null

        // logicConnector 기반 합성 — 첫 번째 condition 의 connector 는 null (단일) 또는 AND/OR
        // 단순화: 모든 connector 가 동일하다고 가정 (혼합은 SF describe 운영상 부재).
        // 혼합 케이스는 향후 정밀화 — 본 spec 1차 구현은 majority connector 채택.
        val majority = rule.conditions.mapNotNull { it.logicConnector }.firstOrNull() ?: "AND"
        return when (majority) {
            "OR" -> conditionPredicates.reduce { acc, e -> acc.or(e) }
            else -> conditionPredicates.reduce { acc, e -> acc.and(e) } // AND default
        }
    }

    /**
     * 단일 condition → QueryDSL Predicate. 11 operator 지원.
     *
     * SF API name (예: `CostCenterCode__c`) → JPA property (`costCenterCode`) 변환은 본 메서드에서 처리.
     */
    internal fun buildConditionPredicate(
        cond: SharingRuleSnapshot.ConditionSnapshot,
        entityPath: EntityPathBase<*>,
    ): BooleanExpression? {
        // audit/owner field 는 별도 분기 — sfid 비교 금지 정책. snapshot pre-resolved User.id 와 FK relation Long 비교.
        if (cond.field in AUDIT_OWNER_FIELDS) {
            return buildAuditOwnerConditionPredicate(cond, entityPath)
        }

        val property = resolveConditionProperty(cond.field, entityPath) ?: run {
            log.debug(
                "[sharing-policy] entity {} has no property for field={} — skip condition predicate",
                entityPath, cond.field,
            )
            return null
        }
        val value = cond.value ?: return null

        return try {
            val path = Expressions.stringPath(entityPath, property)
            // SF sharing rule UI 의 equals / notEqual 은 콤마 입력 시 IN / NOT IN 으로 작동 — 운영 표준 (X5832 `BranchCode__c equals '3844,5832'` 등 다수 rule).
            val splitValues = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            when (cond.operator) {
                "equals" -> if (splitValues.size <= 1) path.eq(value) else path.`in`(splitValues)
                "notEqual" -> if (splitValues.size <= 1) path.ne(value) else path.`in`(splitValues).not()
                "lessThan" -> path.lt(value)
                "greaterThan" -> path.gt(value)
                "lessOrEqual" -> path.loe(value)
                "greaterOrEqual" -> path.goe(value)
                "contains" -> path.contains(value)
                "notContain" -> path.contains(value).not()
                "startsWith" -> path.startsWith(value)
                "includes" -> path.`in`(splitValues)
                "excludes" -> path.`in`(splitValues).not()
                else -> throw IllegalStateException(
                    "Unknown sharingRule operator: ${cond.operator} (field=${cond.field}, value=$value)",
                )
            }
        } catch (e: IllegalStateException) {
            // unknown operator — re-throw (L4 정정)
            throw e
        } catch (e: Exception) {
            log.warn(
                "[sharing-policy] condition predicate build failed — field={} operator={} : {}",
                cond.field, cond.operator, e.message,
            )
            null
        }
    }

    /**
     * SF API name → JPA property 변환.
     * - `CostCenterCode__c` → `costCenterCode`
     * - `AccountGroup__c` → `accountGroup`
     * - `CreatedById` → `createdById`
     * - `BranchCode__c` → `branchCode`
     * - `HR_Code_c__c` → `hrCode` (특수 케이스 — 운영 데이터 정합)
     */
    internal fun sfApiNameToJpaProperty(sfFieldName: String): String {
        // `__c` 제거
        val stripped = sfFieldName.removeSuffix("__c")
        // HR_Code_c 의 변형 처리
        val normalized = stripped.replace("_c$".toRegex(), "")
        // PascalCase → camelCase + 언더스코어 처리
        val parts = normalized.split("_")
        return if (parts.size == 1) {
            normalized.replaceFirstChar { it.lowercase() }
        } else {
            parts.first().replaceFirstChar { it.lowercase() } +
                parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    /**
     * audit/owner field condition 평가 — `createdBy.id` / `lastModifiedBy.id` / `ownerUser.id` Long FK 비교.
     *
     * SF 의 condition value 는 SF user sfid 인데 application 정책상 sfid 직접 매칭 금지.
     * [SharingRuleSnapshot.ConditionSnapshot.resolvedUserId] 가 snapshot 적재 시점에 신규 User.id 로 미리 변환됨 —
     * 본 메서드는 그 Long 과 entity 의 FK relation `.id` 를 비교한다.
     *
     * - resolvedUserId 가 null (sfid 매칭 실패 / 비-sfid value) → condition skip + 경고 로그
     * - entity 에 FK relation 부재 → condition skip + 경고 로그
     * - 지원 operator 는 equals / notEqual 두 종만 (audit field 는 SF UI 에서 사실상 등호 비교만 사용 — `includes` / `excludes`
     *   다중값 케이스가 운영 데이터에 등장하면 본 메서드 확장 필요)
     */
    private fun buildAuditOwnerConditionPredicate(
        cond: SharingRuleSnapshot.ConditionSnapshot,
        entityPath: EntityPathBase<*>,
    ): BooleanExpression? {
        val resolvedUserId = cond.resolvedUserId ?: run {
            log.debug(
                "[sharing-policy] audit field {} value={} not resolved to User.id — skip condition",
                cond.field, cond.value,
            )
            return null
        }
        val relationPath = resolveAuditOwnerRelationPath(cond.field, entityPath) ?: run {
            log.debug(
                "[sharing-policy] entity {} has no FK relation for audit field {} — skip condition",
                entityPath, cond.field,
            )
            return null
        }
        return try {
            @Suppress("UNCHECKED_CAST")
            val path = Expressions.numberPath(java.lang.Long::class.java, entityPath, relationPath)
            when (cond.operator) {
                "equals" -> path.eq(resolvedUserId as java.lang.Long)
                "notEqual" -> path.ne(resolvedUserId as java.lang.Long)
                else -> {
                    log.warn(
                        "[sharing-policy] unsupported operator={} for audit field {} — skip condition",
                        cond.operator, cond.field,
                    )
                    null
                }
            }
        } catch (e: Exception) {
            log.warn(
                "[sharing-policy] audit condition predicate build failed — field={} relation={} : {}",
                cond.field, relationPath, e.message,
            )
            null
        }
    }

    /**
     * ControlledByParent SObject 의 read query — Repository 호출측이 parent entityPath 전달.
     * `buildPredicate(scope, parentSObjectName, parentEntityPath)` 형태로 동일 메서드 재사용.
     */
    fun isControlledByParent(sObjectName: String): Boolean = sObjectSettingProvider.isControlledByParent(sObjectName)

    fun parentSObjectOf(sObjectName: String): String? = sObjectSettingProvider.parentSObjectOf(sObjectName)

    companion object {
        private val AUDIT_OWNER_FIELDS: Set<String> = setOf(
            "CreatedById",
            "LastModifiedById",
            "OwnerId",
        )
    }
}
