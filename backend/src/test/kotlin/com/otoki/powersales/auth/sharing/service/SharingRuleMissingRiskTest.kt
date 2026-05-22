package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.account.entity.QAccount
import com.otoki.powersales.auth.sharing.dto.SharingRuleSnapshot
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

    private fun toJpql(expr: com.querydsl.core.types.dsl.BooleanExpression): String {
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
    @DisplayName("CreatedById 예외 — DisplayWorkSchedule / MFEIS 의 audit 컬럼 분기")
    inner class CreatedByIdException {

        @Test
        @DisplayName("CreatedById equals condition → createdById JPA property 매핑 + JPQL `=` 합성")
        fun createdByIdEquals() {
            // SF rule 본문 예: field="CreatedById" operator="equals" value="005xx..." (사용자 sfid)
            val cond = SharingRuleSnapshot.ConditionSnapshot(
                field = "CreatedById",
                operator = "equals",
                value = "005ABC123",
                conditionOrder = 1,
                logicConnector = null,
            )
            val expr = evaluator.buildConditionPredicate(cond, account)
            assertThat(expr).isNotNull
            // CreatedById → createdById 변환 + equals → JPQL `=` 합성 정상.
            // (CreatedById 가 audit 컬럼인 BaseEntity 추상화 매핑은 entity 별 책임 — 변환 자체만 본 spec 검증.)
            assertThat(toJpql(expr!!)).contains("account.createdById = ?1")
        }

        @Test
        @DisplayName("CreatedById in (...) — includes operator 분기 (Group 멤버십 시나리오)")
        fun createdByIdIncludes() {
            // CreatedById IN (csv) — 본인 + 동일 Group 동료가 작성한 record 가시 (DisplayWorkSchedule 의 CreatedById 예외 패턴)
            val cond = SharingRuleSnapshot.ConditionSnapshot(
                field = "CreatedById",
                operator = "includes",
                value = "005ABC, 005DEF, 005GHI",
                conditionOrder = 1,
                logicConnector = null,
            )
            val expr = evaluator.buildConditionPredicate(cond, account)
            assertThat(expr).isNotNull
            // includes → csv split → IN 절 합성
            assertThat(toJpql(expr!!)).contains("account.createdById in")
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
        @DisplayName("CVS rule (AND 합성) — AccountGroup + CostCenterCode 동시 조건")
        fun cvsAndComposition() {
            // CVS rule 본문이 AccountGroup__c + CostCenterCode__c 두 condition 을 AND 로 묶는 패턴.
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
                        field = "CostCenterCode__c",
                        operator = "equals",
                        value = "DKB100",
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
            assertThat(jpql).contains("account.costCenterCode = ?2")
            assertThat(jpql).contains("and")
        }
    }
}
