package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.domain.foundation.account.entity.QAccount
import com.otoki.powersales.platform.auth.sharing.entity.QPermissionSetAssignment
import com.otoki.powersales.schedule.entity.QDisplayWorkSchedule
import com.otoki.powersales.platform.auth.sharing.dto.SharingRuleSnapshot
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.HQLTemplates
import com.querydsl.jpa.JPQLSerializer
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 누락 위험 5건 중 evaluator Predicate 분기 검증 가능 2건 (spec #786 P1-B / Q3 옵션 1).
 *
 * - CreatedById 예외 (DisplayWorkSchedule / MFEIS) — `field=CreatedById` condition 이 evaluator 의 SF API name →
 *   JPA property 변환 (`CreatedById → createdById`) + Predicate 합성에 정상 반영되는지 검증.
 * - CVS (Conditional View Sharing) — Account 의 `AccountGroup__c` condition (1000/1010/3000 사업부) rule 이
 *   evaluator 의 `includes` operator 분기 + Predicate 합성에 정상 반영되는지 검증.
 *
 * 나머지 누락 위험 3건 (HR 부서코드 / Edit-Read 이중 레이어 / SkipFkPrefixes) 은 각 도메인 spec 책임 (Q3 옵션 1).
 *
 * 검증 도구: JPQLSerializer + predicate.toString(). testcontainers / 실 DB 의존 0건.
 */
@DisplayName("Sharing Rule 누락 위험 — CreatedById 예외 + CVS 분기 (spec #786)")
class SharingRuleMissingRiskTest {

    private val evaluator = SharingRulePolicyEvaluator(mockk(relaxed = true))
    private val account = QAccount.account
    // CostCenterCode 검증용 — DisplayWorkSchedule 보유 entity
    private val displayWorkSchedule = QDisplayWorkSchedule.displayWorkSchedule
    // CreatedById 단순 필드 검증용 — PermissionSetAssignment 가 createdById 직접 필드 보유
    private val psaPath = QPermissionSetAssignment.permissionSetAssignment

    private fun toJpql(expr: BooleanExpression): String {
        val serializer = JPQLSerializer(HQLTemplates.DEFAULT)
        serializer.handle(expr)
        return serializer.toString()
    }

    private fun rule(developerName: String, conditions: List<SharingRuleSnapshot.ConditionSnapshot>): SharingRuleSnapshot =
        SharingRuleSnapshot(
            sharingRuleId = 1L,
            developerName = developerName,
            sObjectName = "Account",
            ruleType = "CRITERIA",
            accessLevel = "Read",
            includeOwnedByAll = false,
            conditions = conditions,
        )

    @Nested
    @DisplayName("CreatedById 예외 — audit FK relation 분기 (sfid 직접 매칭 금지 정책)")
    inner class CreatedByIdException {

        @Test
        @DisplayName("CreatedById equals + resolvedUserId 있음 → createdBy.id FK Long 비교 (DWSM entity)")
        fun createdByIdEqualsWithResolvedUserId() {
            // SF rule 본문 예: field="CreatedById" operator="equals" value="005xx..." (사용자 sfid)
            // 신규 시스템 정책: sfid 직접 매칭 금지. snapshot loader 가 sfid → User.id 로 pre-resolve.
            // evaluator 는 resolvedUserId (Long) 와 entity 의 FK relation `createdBy.id` 를 비교.
            val cond = SharingRuleSnapshot.ConditionSnapshot(
                field = "CreatedById",
                operator = "equals",
                value = "005ABC123",
                conditionOrder = 1,
                logicConnector = null,
                resolvedUserId = 42L,
            )
            val expr = evaluator.buildConditionPredicate(cond, displayWorkSchedule)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.createdBy.id = ?1")
        }

        @Test
        @DisplayName("CreatedById + resolvedUserId 없음 → condition skip (User 매칭 실패)")
        fun createdByIdWithoutResolvedUserIdSkips() {
            // sfid 가 신규 User table 에 없거나 value 가 비-sfid 패턴이면 snapshot loader 가 resolvedUserId = null.
            // evaluator 는 condition skip + 경고 로그. GBA01_J/GGA22_J 의 DWSM sharing rule 누락 가능 — 의도적 정합.
            val cond = SharingRuleSnapshot.ConditionSnapshot(
                field = "CreatedById",
                operator = "equals",
                value = "005ABC123",
                conditionOrder = 1,
                logicConnector = null,
                resolvedUserId = null,
            )
            val expr = evaluator.buildConditionPredicate(cond, displayWorkSchedule)
            assertThat(expr).isNull()
        }
    }

    @Nested
    @DisplayName("CVS — Conditional View Sharing — Account 의 AccountGroup__c 사업부 분기")
    inner class ConditionalViewSharing {

        @Test
        @DisplayName("AccountGroup__c includes 1000,1010,3000 → 사업부 사용자별 가시성 분기")
        fun cvsAccountGroupIncludes() {
            // CVS rule 본문 예: field="AccountGroup__c" operator="includes" value="1000, 1010, 3000"
            val cond = SharingRuleSnapshot.ConditionSnapshot(
                field = "AccountGroup__c",
                operator = "includes",
                value = "1000, 1010, 3000",
                conditionOrder = 1,
                logicConnector = null,
            )
            val expr = evaluator.buildConditionPredicate(cond, account)
            assertThat(expr).isNotNull
            // AccountGroup__c → accountGroup 변환 + includes 분기 → IN 절 합성
            assertThat(toJpql(expr!!)).contains("account.accountGroup in")
            assertThat(expr.toString()).contains("1000").contains("1010").contains("3000")
        }

        @Test
        @DisplayName("CVS rule (AND 합성) — AccountGroup + ABCType 동시 조건 (Account 정합 field)")
        fun cvsAndComposition() {
            // SF Account.sharingRules 운영 표준 — AccountGroup__c + BranchCode__c 패턴 (X5832 등).
            // Account 에 costCenterCode 부재 — 본 test 는 AND 합성 검증 의도라 entity 정합 field 로 대체.
            val cvsRule = rule(
                "CVS_AccountGroup_Sales",
                listOf(
                    SharingRuleSnapshot.ConditionSnapshot(
                        field = "AccountGroup__c",
                        operator = "equals",
                        value = "1000",
                        conditionOrder = 1,
                        logicConnector = null,
                    ),
                    SharingRuleSnapshot.ConditionSnapshot(
                        field = "BranchCode__c",
                        operator = "equals",
                        value = "5832",
                        conditionOrder = 2,
                        logicConnector = "AND",
                    ),
                ),
            )
            val expr = evaluator.buildCriteriaRulePredicate(cvsRule, account)
            assertThat(expr).isNotNull
            val jpql = toJpql(expr!!)
            // AND 합성 — 두 조건 모두 포함 + and 연산자
            assertThat(jpql).contains("account.accountGroup = ?1")
            assertThat(jpql).contains("account.branchCode = ?2")
            assertThat(jpql).contains("and")
        }
    }
}
