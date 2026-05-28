package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.schedule.entity.QDisplayWorkSchedule
import com.otoki.powersales.auth.sharing.dto.SharingRuleSnapshot
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.HQLTemplates
import com.querydsl.jpa.JPQLSerializer
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SharingRulePolicyEvaluator 의 11 operator 가 QueryDSL → JPQL 로 의도대로 변환되는지 검증 (spec #786 P1-B).
 *
 * ## 검증 책임 분리
 * - **우리 책임 (본 테스트)**: condition (`field`, `operator`, `value`) 입력에서 의도한 QueryDSL BooleanExpression 합성
 * - **QueryDSL / Hibernate 책임**: BooleanExpression → JPQL → SQL 컴파일 (신뢰)
 * - **PostgreSQL 책임**: SQL 실행 결과 (신뢰)
 *
 * 본 테스트는 첫 번째 책임만 검증. testcontainers / 실 DB 의존 0건.
 *
 * ## 검증 도구
 * - `JPQLSerializer(HQLTemplates.DEFAULT)` — 운영 Hibernate 가 사용하는 동일 컴파일러 (Q1 옵션 1 채택)
 * - `predicate.toString()` — 가독성 보조
 */
@DisplayName("SharingRuleOperator — 11 operator JPQL 변환 검증 (spec #786)")
class SharingRuleOperatorTest {

    private val evaluator = SharingRulePolicyEvaluator(mockk(relaxed = true))
    // entity path — costCenterCode 를 가진 운영 entity 선택 (Account 부재)
    private val entity = QDisplayWorkSchedule.displayWorkSchedule

    /**
     * BooleanExpression 의 JPQL 직렬화 결과 반환.
     */
    private fun toJpql(expr: BooleanExpression): String {
        val serializer = JPQLSerializer(HQLTemplates.DEFAULT)
        serializer.handle(expr)
        return serializer.toString()
    }

    private fun cond(operator: String, value: String?): SharingRuleSnapshot.ConditionSnapshot =
        SharingRuleSnapshot.ConditionSnapshot(
            field = "CostCenterCode__c",
            operator = operator,
            value = value,
            conditionOrder = 1,
            logicConnector = null,
        )

    @Nested
    @DisplayName("equality operators")
    inner class EqualityOperators {

        @Test
        @DisplayName("equals → `column = ?`")
        fun equals() {
            val expr = evaluator.buildConditionPredicate(cond("equals", "X"), entity)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode = ?1")
            assertThat(expr.toString()).contains("displayWorkSchedule.costCenterCode = X")
        }

        @Test
        @DisplayName("notEqual → JPQL `column <> ?` / QueryDSL toString `column != ?`")
        fun notEqual() {
            val expr = evaluator.buildConditionPredicate(cond("notEqual", "X"), entity)
            assertThat(expr).isNotNull
            // JPQLSerializer (운영 SQL 정합) 은 JPQL 표준 `<>` 로 직렬화
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode <> ?1")
            // QueryDSL 내부 toString() 은 Java 식 `!=` 표현 — 가독성 보조
            assertThat(expr.toString()).contains("displayWorkSchedule.costCenterCode != X")
        }
    }

    @Nested
    @DisplayName("comparison operators")
    inner class ComparisonOperators {

        @Test
        @DisplayName("lessThan → `column < ?`")
        fun lessThan() {
            val expr = evaluator.buildConditionPredicate(cond("lessThan", "100"), entity)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode < ?1")
        }

        @Test
        @DisplayName("greaterThan → `column > ?`")
        fun greaterThan() {
            val expr = evaluator.buildConditionPredicate(cond("greaterThan", "100"), entity)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode > ?1")
        }

        @Test
        @DisplayName("lessOrEqual → `column <= ?`")
        fun lessOrEqual() {
            val expr = evaluator.buildConditionPredicate(cond("lessOrEqual", "100"), entity)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode <= ?1")
        }

        @Test
        @DisplayName("greaterOrEqual → `column >= ?`")
        fun greaterOrEqual() {
            val expr = evaluator.buildConditionPredicate(cond("greaterOrEqual", "100"), entity)
            assertThat(expr).isNotNull
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode >= ?1")
        }
    }

    @Nested
    @DisplayName("string match operators")
    inner class StringMatchOperators {

        @Test
        @DisplayName("contains → JPQL `column like ?1 escape '!'` / toString `contains(column,value)`")
        fun contains() {
            val expr = evaluator.buildConditionPredicate(cond("contains", "abc"), entity)
            assertThat(expr).isNotNull
            // JPQLSerializer 는 이미 SQL LIKE 형식으로 직렬화 (escape character 박제)
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode like ?1 escape '!'")
            // QueryDSL 내부 toString() 은 함수 형태 — 가독성 보조
            assertThat(expr.toString()).isEqualTo("contains(displayWorkSchedule.costCenterCode,abc)")
        }

        @Test
        @DisplayName("notContain → JPQL `not(column like ?1 escape '!')` / toString `!contains(...)`")
        fun notContain() {
            val expr = evaluator.buildConditionPredicate(cond("notContain", "abc"), entity)
            assertThat(expr).isNotNull
            val jpql = toJpql(expr!!)
            assertThat(jpql).contains("displayWorkSchedule.costCenterCode like ?1 escape '!'")
            // not 합성 — JPQL 또는 toString 어느 한쪽에서 not / ! prefix 확인
            assertThat(expr.toString()).startsWith("!contains(displayWorkSchedule.costCenterCode,")
        }

        @Test
        @DisplayName("startsWith → JPQL `column like ?1 escape '!'` / toString `startsWith(column,value)`")
        fun startsWith() {
            val expr = evaluator.buildConditionPredicate(cond("startsWith", "abc"), entity)
            assertThat(expr).isNotNull
            // JPQLSerializer 는 이미 SQL LIKE 형식으로 직렬화 (`value%` pattern 은 Hibernate 가 처리)
            assertThat(toJpql(expr!!)).contains("displayWorkSchedule.costCenterCode like ?1 escape '!'")
            assertThat(expr.toString()).isEqualTo("startsWith(displayWorkSchedule.costCenterCode,abc)")
        }
    }

    @Nested
    @DisplayName("set membership operators")
    inner class SetMembershipOperators {

        @Test
        @DisplayName("includes → `column in (?, ?, ...)` (csv split)")
        fun includes() {
            val expr = evaluator.buildConditionPredicate(cond("includes", "X, Y, Z"), entity)
            assertThat(expr).isNotNull
            val jpql = toJpql(expr!!)
            assertThat(jpql).contains("displayWorkSchedule.costCenterCode in")
            // csv split 의 trim 동작 — toString 에 X / Y / Z 3개 항목 노출
            val rendered = expr.toString()
            assertThat(rendered).contains("X").contains("Y").contains("Z")
        }

        @Test
        @DisplayName("excludes → `not(column in (?, ?, ...))`")
        fun excludes() {
            val expr = evaluator.buildConditionPredicate(cond("excludes", "X,Y,Z"), entity)
            assertThat(expr).isNotNull
            val jpql = toJpql(expr!!)
            assertThat(jpql).contains("displayWorkSchedule.costCenterCode in")
            // `path.in(values).not()` 합성 — toString 에 not 접두 분기 포함
            assertThat(expr.toString()).startsWith("!")
        }
    }

    @Nested
    @DisplayName("error / null handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("unknown operator → IllegalStateException (re-throw, L4 정정)")
        fun unknownOperator() {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
                evaluator.buildConditionPredicate(cond("UNSUPPORTED_OP", "X"), entity)
            }
        }

        @Test
        @DisplayName("value = null → null Predicate (조건 합성 생략)")
        fun nullValue() {
            val expr = evaluator.buildConditionPredicate(cond("equals", null), entity)
            assertThat(expr).isNull()
        }
    }
}
