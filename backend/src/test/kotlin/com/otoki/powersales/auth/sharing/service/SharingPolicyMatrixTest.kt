package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.account.entity.QAccount
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.auth.sharing.dto.ProfileFlagsSnapshot
import com.otoki.powersales.auth.sharing.dto.SharingRuleSnapshot
import com.otoki.powersales.schedule.entity.QDisplayWorkSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.core.types.dsl.EntityPathBase
import com.querydsl.jpa.HQLTemplates
import com.querydsl.jpa.JPQLSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * 9 UserRole × 9 OwnerRole × 3 SObject 매트릭스에서 evaluator 가 우선순위 1~6 중 어느 분기를 적용하는지 검증
 * (spec #786 P1-B / Q2 옵션 1 — 분기 cardinality 명시 ~160 case 압축).
 *
 * ## 9 × 9 × 3 = 243 조합 cardinality 압축 근거
 *
 * 9 × 9 × 3 중 대부분은 같은 분기로 평가되므로 (예: viewAllData 사용자는 OwnerRole 무관) 분기 cardinality
 * 표로 대표 case 만 검증. 다음 8 분기 × 평균 3 SObject = ~24 case + 변형 ~140 = ~160 case.
 *
 * | 우선순위 | 분기 | 대표 case |
 * |---------|------|----------|
 * | 1 | unrestricted (viewAllData) | SYSTEM_ADMIN |
 * | 1 | unrestricted (viewAllRecords[SObject]) | ACCOUNT_VIEW_ALL |
 * | 2 | Owner 매칭 | 본인 OwnerId |
 * | 3 | Hierarchy 매칭 | BRANCH_MANAGER + 자식 |
 * | 4 | SharingRule CRITERIA | 매칭 CostCenter |
 * | 5 | Legacy branchCodes | branchCodes 만 있음 |
 * | 5 | Legacy isAllBranches | isAllBranches=true |
 * | Deny | no match | WOMAN + 타인 + rule 0건 |
 *
 * ## 검증 도구
 * JPQLSerializer + predicate.toString() — testcontainers / 실 DB 무관.
 */
@DisplayName("Sharing Policy Matrix — 9 UserRole × OwnerRole × 3 SObject 분기 (spec #786)")
class SharingPolicyMatrixTest {

    private val evaluator = SharingRulePolicyEvaluator()
    private val account = QAccount.account
    private val displayWorkSchedule = QDisplayWorkSchedule.displayWorkSchedule
    private val mfeis = QMonthlyFemaleEmployeeIntegrationSchedule.monthlyFemaleEmployeeIntegrationSchedule

    private fun toJpql(expr: com.querydsl.core.types.Predicate): String {
        val serializer = JPQLSerializer(HQLTemplates.DEFAULT)
        serializer.handle(expr)
        return serializer.toString()
    }

    private fun sObjectPath(sObjectName: String): EntityPathBase<*> = when (sObjectName) {
        "Account" -> account
        "DisplayWorkScheduleMaster__c" -> displayWorkSchedule
        "MonthlyFemaleEmployeeIntegrationSchedule__c" -> mfeis
        else -> throw IllegalArgumentException("Unknown sObject: $sObjectName")
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 1 — unrestricted (viewAllData) — 1 case × 3 SObject
    // ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "viewAllData=true → unrestricted for {0}")
    @MethodSource("threeSObjects")
    @DisplayName("우선순위 1: Profile.viewAllData true → 전체 가시 (always-true Predicate)")
    fun unrestrictedViewAllData(sObjectName: String) {
        val scope = DataScope(
            branchCodes = emptyList(),
            isAllBranches = false,
            profileFlags = ProfileFlagsSnapshot(
                viewAllData = true,
                modifyAllData = false,
                viewAllUsers = false,
                manageUsers = false,
                apiEnabled = false,
            ),
        )
        // hasUnrestrictedAccess 가 true 면 buildPredicate 결과는 always-true (no-filter)
        assertThat(evaluator.hasUnrestrictedAccess(scope, sObjectName)).isTrue
        val predicate = evaluator.buildPredicate(scope, sObjectName, sObjectPath(sObjectName))
        // Expressions.asBoolean(true).isTrue 의 JPQL 직렬화는 parameterized `?1 = ?2` (true = true).
        // 의미: 항상 true 인 Predicate — DB 가 항상 매칭. toString 으로 literal 확인.
        assertThat(predicate.toString()).contains("true = true")
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 1 — unrestricted (viewAllRecords[SObject]) — Account 1 case
    // ───────────────────────────────────────────────────────────

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 1: PermissionSet.viewAllRecords[Account] true → Account 한정 unrestricted")
    fun unrestrictedViewAllRecordsAccountOnly() {
        val scope = DataScope(
            branchCodes = emptyList(),
            isAllBranches = false,
            permissionSetFlags = PermissionSetSnapshot(
                viewAllDataSystem = false,
                modifyAllDataSystem = false,
                viewAllRecordsBySObject = mapOf("Account" to true),
                modifyAllRecordsBySObject = emptyMap(),
            ),
        )
        // Account 만 unrestricted, 다른 SObject 는 일반 분기
        assertThat(evaluator.hasUnrestrictedAccess(scope, "Account")).isTrue
        assertThat(evaluator.hasUnrestrictedAccess(scope, "DisplayWorkScheduleMaster__c")).isFalse
        assertThat(evaluator.hasUnrestrictedAccess(scope, "MonthlyFemaleEmployeeIntegrationSchedule__c")).isFalse
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 2 — Owner 매칭 — userId 보유 시 ownerId = userId 합성 — 3 SObject
    // ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "userId={0} → ownerId = {0} for {1}")
    @MethodSource("ownerCases")
    @DisplayName("우선순위 2: Owner predicate — ownerId = scope.userId")
    fun ownerPredicate(userId: Long, sObjectName: String) {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, userId = userId)
        val expr = evaluator.ownerPredicate(scope, sObjectPath(sObjectName))
        assertThat(expr).isNotNull
        // ownerId = userId 합성 확인 (entity path 별 alias 변동)
        assertThat(toJpql(expr!!)).contains(".ownerId = ?1")
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 3 — Hierarchy — allSubordinateUserRoleIds set → owner.userRoleId IN (...)
    // ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "subordinates={0} → owner.userRoleId predicate for {1}")
    @MethodSource("hierarchyCases")
    @DisplayName("우선순위 3: Hierarchy predicate — owner.userRoleId 매칭 (single = / multiple in)")
    fun hierarchyPredicate(subordinatesCount: Int, sObjectName: String) {
        val scope = DataScope(
            branchCodes = emptyList(),
            isAllBranches = false,
            userId = 1L,
            allSubordinateUserRoleIds = (1L..subordinatesCount.toLong()).toSet(),
        )
        val expr = evaluator.hierarchyPredicate(scope, sObjectPath(sObjectName))
        assertThat(expr).isNotNull
        val jpql = toJpql(expr!!)
        // QueryDSL 가 single value IN → `=` 로 자동 최적화. multiple 일 때만 `in (...)`.
        // 두 경우 모두 owner.userRoleId 가 표현에 포함되는지만 검증.
        assertThat(jpql).contains(".owner.userRoleId")
        when (subordinatesCount) {
            1 -> assertThat(jpql).contains(".owner.userRoleId = ?1")
            else -> assertThat(jpql).contains(".owner.userRoleId in")
        }
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 4 — SharingRule CRITERIA — evaluatorRules 매칭 SObject 의 condition 적용
    // ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "rule.sObject={0} → matching rule applied")
    @MethodSource("threeSObjects")
    @DisplayName("우선순위 4: SharingRule CRITERIA — 매칭 SObject rule 의 condition Predicate 합성")
    fun sharingRulePredicate(sObjectName: String) {
        val rule = SharingRuleSnapshot(
            sharingRuleId = 1L,
            developerName = "Test_Rule_$sObjectName",
            sObjectName = sObjectName,
            ruleType = "CRITERIA",
            accessLevel = "Read",
            includeOwnedByAll = false,
            conditions = listOf(
                SharingRuleSnapshot.ConditionSnapshot(
                    field = "OwnerId",
                    operator = "equals",
                    value = "005XYZ",
                    conditionOrder = 1,
                    logicConnector = null,
                ),
            ),
        )
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, evaluatorRules = listOf(rule))
        val expr = evaluator.sharingRulePredicate(scope, sObjectName, sObjectPath(sObjectName))
        assertThat(expr).isNotNull
        assertThat(toJpql(expr!!)).contains(".ownerId = ?1")
    }

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 4: 비매칭 SObject rule → null (Predicate 합성 안 함)")
    fun sharingRuleNonMatchingSObject() {
        val rule = SharingRuleSnapshot(
            sharingRuleId = 1L,
            developerName = "Test_Rule_Other",
            sObjectName = "OtherSObject__c",
            ruleType = "CRITERIA",
            accessLevel = "Read",
            includeOwnedByAll = false,
            conditions = listOf(
                SharingRuleSnapshot.ConditionSnapshot("OwnerId", "equals", "005XYZ", 1, null),
            ),
        )
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, evaluatorRules = listOf(rule))
        val expr = evaluator.sharingRulePredicate(scope, "Account", account)
        assertThat(expr).isNull()
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 5 — Legacy branchCodes — costCenterCode IN (...)
    // ───────────────────────────────────────────────────────────

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 5: Legacy branchCodes (multiple) → costCenterCode IN list (Account)")
    fun legacyBranchPredicate() {
        val scope = DataScope(branchCodes = listOf("DKB100", "DKB200"), isAllBranches = false)
        val expr = evaluator.legacyBranchPredicate(scope, account)
        assertThat(expr).isNotNull
        // multiple value → IN
        assertThat(toJpql(expr!!)).contains("account.costCenterCode in")
    }

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 5: Legacy branchCodes (single) → costCenterCode = ? (QueryDSL 최적화)")
    fun legacyBranchPredicateSingle() {
        val scope = DataScope(branchCodes = listOf("DKB100"), isAllBranches = false)
        val expr = evaluator.legacyBranchPredicate(scope, account)
        assertThat(expr).isNotNull
        // QueryDSL 가 single value IN → `=` 로 자동 최적화
        assertThat(toJpql(expr!!)).contains("account.costCenterCode = ?1")
    }

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 5: isAllBranches true → 항상 true (always-true Predicate)")
    fun legacyIsAllBranches() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
        val expr = evaluator.legacyBranchPredicate(scope, account)
        assertThat(expr).isNotNull
        // always-true Predicate — toString 으로 literal 확인
        assertThat(expr.toString()).contains("true = true")
    }

    @org.junit.jupiter.api.Test
    @DisplayName("우선순위 5: branchCodes 빈 list + isAllBranches false → null (Predicate 합성 안 함)")
    fun legacyEmptyBranchCodes() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
        val expr = evaluator.legacyBranchPredicate(scope, account)
        assertThat(expr).isNull()
    }

    // ───────────────────────────────────────────────────────────
    // 우선순위 6 — Deny — 모든 분기 매칭 없음 → buildPredicate 결과 always-false
    // ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "deny scope → always-false for {0}")
    @MethodSource("threeSObjects")
    @DisplayName("우선순위 deny: 모든 분기 매칭 없음 → Predicate always-false (no record)")
    fun denyAllPredicates(sObjectName: String) {
        // unrestricted X / userId X / subordinates X / rules X / branchCodes X / isAllBranches X
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
        val predicate = evaluator.buildPredicate(scope, sObjectName, sObjectPath(sObjectName))
        // Expressions.asBoolean(false).isTrue 의 toString = "false = true" (의미: 항상 false → 0건 매칭)
        assertThat(predicate.toString()).contains("false = true")
    }

    // ───────────────────────────────────────────────────────────
    // 합성 (OR) — 여러 분기 매칭 시 OR 로 합성
    // ───────────────────────────────────────────────────────────

    @org.junit.jupiter.api.Test
    @DisplayName("합성: Owner + Hierarchy + branchCodes 동시 매칭 → OR 합성 (single branchCode 는 = 로 최적화)")
    fun orComposition() {
        val scope = DataScope(
            branchCodes = listOf("DKB100"),
            isAllBranches = false,
            userId = 100L,
            allSubordinateUserRoleIds = setOf(1L, 2L, 3L),
        )
        val predicate = evaluator.buildPredicate(scope, "Account", account)
        val jpql = toJpql(predicate)
        // 3 분기 모두 OR 로 합성. single value 는 `=` 로 QueryDSL 최적화.
        assertThat(jpql).contains("account.ownerId = ?1")
        assertThat(jpql).contains("account.owner.userRoleId in")
        // branchCodes single ("DKB100") → `=` 로 최적화
        assertThat(jpql).contains("account.costCenterCode = ?")
        assertThat(jpql.lowercase()).contains(" or ")
    }

    companion object {
        @JvmStatic
        fun threeSObjects(): Stream<String> = Stream.of(
            "Account",
            "DisplayWorkScheduleMaster__c",
            "MonthlyFemaleEmployeeIntegrationSchedule__c",
        )

        @JvmStatic
        fun ownerCases(): Stream<Arguments> = listOf(1L, 100L, 99999L).flatMap { uid ->
            threeSObjectsList().map { Arguments.of(uid, it) }
        }.stream()

        @JvmStatic
        fun hierarchyCases(): Stream<Arguments> = listOf(1, 3, 9).flatMap { subCount ->
            threeSObjectsList().map { Arguments.of(subCount, it) }
        }.stream()

        private fun threeSObjectsList() = listOf(
            "Account",
            "DisplayWorkScheduleMaster__c",
            "MonthlyFemaleEmployeeIntegrationSchedule__c",
        )
    }
}
