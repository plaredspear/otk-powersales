package com.otoki.powersales.domain.foundation.account.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [ClosedAccountSalesExemption] — 폐업 거래처 면제 정책의 단일 출처.
 *
 * 거래처 lookup (SQL predicate) 과 진열사원스케줄 등록 검증 (엔티티 판정) 이 이 정책을 공유하므로,
 * 경계 분기(연말·연초, picklist 범위 밖) 를 여기서 직접 고정한다.
 */
@DisplayName("ClosedAccountSalesExemption — 폐업 면제 정책")
class ClosedAccountSalesExemptionTest {

    @Nested
    @DisplayName("recentYearMonths — 기준월 산출")
    inner class RecentYearMonths {

        @Test
        @DisplayName("당월 + 전월 2건을 (년, 월) 쌍으로 반환")
        fun returnsCurrentAndPreviousMonth() {
            val pairs = ClosedAccountSalesExemption.recentYearMonths(LocalDate.of(2026, 8, 13))

            assertThat(pairs).containsExactly(
                SalesYear.Y2026 to SalesMonth.M08,
                SalesYear.Y2026 to SalesMonth.M07,
            )
        }

        @Test
        @DisplayName("1월 기준 — 전월이 전년 12월로 넘어감 (연초 경계)")
        fun crossesYearBoundaryInJanuary() {
            val pairs = ClosedAccountSalesExemption.recentYearMonths(LocalDate.of(2026, 1, 15))

            assertThat(pairs).containsExactly(
                SalesYear.Y2026 to SalesMonth.M01,
                SalesYear.Y2025 to SalesMonth.M12,
            )
        }

        @Test
        @DisplayName("SalesYear picklist 범위(2019~2030) 밖이면 빈 목록 — 매출 면제 무효화")
        fun returnsEmptyOutsidePicklistRange() {
            assertThat(ClosedAccountSalesExemption.recentYearMonths(LocalDate.of(2031, 6, 1))).isEmpty()
        }

        @Test
        @DisplayName("범위 경계 진입월(2019-01) — 전월(2018-12)만 탈락")
        fun dropsOnlyOutOfRangeSide() {
            val pairs = ClosedAccountSalesExemption.recentYearMonths(LocalDate.of(2019, 1, 10))

            assertThat(pairs).containsExactly(SalesYear.Y2019 to SalesMonth.M01)
        }
    }

    @Nested
    @DisplayName("isExemptByAccountAttributes — SF 원본 면제")
    inner class AttributeExemption {

        private fun account(distribution: String? = null, abcTypeCode: String? = null) =
            Account(externalKey = "EXT", name = "거래처", distribution = distribution, abcTypeCode = abcTypeCode)

        @Test
        @DisplayName("distribution 보유 시 면제")
        fun exemptByDistribution() {
            assertThat(ClosedAccountSalesExemption.isExemptByAccountAttributes(account(distribution = "10"))).isTrue()
        }

        @Test
        @DisplayName("ABC유형 3062 면제")
        fun exemptByAbcTypeCode() {
            assertThat(
                ClosedAccountSalesExemption.isExemptByAccountAttributes(
                    account(abcTypeCode = ClosedAccountSalesExemption.ABC_TYPE_CODE_EXEMPT)
                )
            ).isTrue()
        }

        @Test
        @DisplayName("distribution 이 공백 문자열이어도 면제 — SQL predicate(`<> ''`) 와 동일 축")
        fun exemptOnWhitespaceDistribution() {
            // SF 원본 조건이 `notEqual ""` 이라 공백은 "값 있음". isNullOrBlank 로 판정하면 이 거래처가
            // 조회에는 나오고 등록만 반려되어, 이 정책이 없애려는 불일치가 그대로 재현된다.
            assertThat(ClosedAccountSalesExemption.isExemptByAccountAttributes(account(distribution = "  "))).isTrue()
        }

        @Test
        @DisplayName("distribution 이 빈 문자열이면 면제 아님")
        fun notExemptOnEmptyDistribution() {
            assertThat(ClosedAccountSalesExemption.isExemptByAccountAttributes(account(distribution = ""))).isFalse()
        }

        @Test
        @DisplayName("둘 다 없으면 면제 아님")
        fun notExemptWithoutBoth() {
            assertThat(ClosedAccountSalesExemption.isExemptByAccountAttributes(account())).isFalse()
        }

        @Test
        @DisplayName("다른 ABC유형코드는 면제 아님")
        fun notExemptOnOtherAbcTypeCode() {
            assertThat(ClosedAccountSalesExemption.isExemptByAccountAttributes(account(abcTypeCode = "3061"))).isFalse()
        }
    }
}
