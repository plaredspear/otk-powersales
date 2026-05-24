package com.otoki.powersales.schedule.service

import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.querydsl.jpa.impl.JPAQueryFactory
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("TeamMemberScheduleSearchService (Spec 813)")
class TeamMemberScheduleSearchServiceTest {

    private lateinit var expander: BranchCodeExpander
    private lateinit var queryFactory: JPAQueryFactory
    private lateinit var service: TeamMemberScheduleSearchService

    @BeforeEach
    fun setUp() {
        expander = mockk()
        queryFactory = mockk()
        service = TeamMemberScheduleSearchService(expander, queryFactory)
    }

    @Nested
    @DisplayName("closingAmountSum — D3=(a) SF formula `ABCClosingSumAmount + ShipClosingSumAmount`")
    inner class ClosingAmountSum {

        @Test
        @DisplayName("두 컬럼 모두 존재 → 합산")
        fun bothPresent() {
            val row = MonthlySalesHistory(
                abcClosingSumAmount = 100_000.0,
                shipClosingSumAmount = 50_000.0,
            )
            assertThat(service.closingAmountSum(row)).isEqualByComparingTo(BigDecimal("150000"))
        }

        @Test
        @DisplayName("한쪽만 null → 다른쪽만 합산")
        fun oneNull() {
            val row = MonthlySalesHistory(
                abcClosingSumAmount = 100_000.0,
                shipClosingSumAmount = null,
            )
            assertThat(service.closingAmountSum(row)).isEqualByComparingTo(BigDecimal("100000"))
        }

        @Test
        @DisplayName("둘 다 null → 0")
        fun bothNull() {
            val row = MonthlySalesHistory()
            assertThat(service.closingAmountSum(row)).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    @Nested
    @DisplayName("sixMonthRange — SF cls:143-150 (선택월=당월 시 -6~-1, 그 외 -5~0)")
    inner class SixMonthRange {

        @Test
        @DisplayName("선택월 == 당월 → [선택월-6, 선택월-1]")
        fun selectedEqualsCurrent() {
            val today = LocalDate.of(2026, 5, 15)
            val (start, end) = service.sixMonthRange("2026", "5", today)
            assertThat(start).isEqualTo(YearMonth.of(2025, 11))
            assertThat(end).isEqualTo(YearMonth.of(2026, 4))
        }

        @Test
        @DisplayName("선택월 != 당월 (과거) → [선택월-5, 선택월]")
        fun selectedNotCurrent() {
            val today = LocalDate.of(2026, 5, 15)
            val (start, end) = service.sixMonthRange("2026", "3", today)
            assertThat(start).isEqualTo(YearMonth.of(2025, 10))
            assertThat(end).isEqualTo(YearMonth.of(2026, 3))
        }
    }

    @Nested
    @DisplayName("enumerateMonths — start~end 사이 YearMonth 열거 (양끝 포함)")
    inner class EnumerateMonths {

        @Test
        @DisplayName("6개월 연속")
        fun sixConsecutiveMonths() {
            val months = service.enumerateMonths(YearMonth.of(2025, 11), YearMonth.of(2026, 4))
            assertThat(months).hasSize(6).containsExactly(
                YearMonth.of(2025, 11),
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
                YearMonth.of(2026, 3),
                YearMonth.of(2026, 4),
            )
        }

        @Test
        @DisplayName("단일 월")
        fun singleMonth() {
            val months = service.enumerateMonths(YearMonth.of(2026, 5), YearMonth.of(2026, 5))
            assertThat(months).hasSize(1).containsExactly(YearMonth.of(2026, 5))
        }
    }

    @Test
    @DisplayName("SalesYear/SalesMonth enum 형식 사전 검증 — sixMonthRange 산출 결과가 enum 으로 안전 변환 가능")
    fun enumFormatSanityCheck() {
        // Y2026 / M05 등 enum name 컨벤션이 sixMonthRange 가 산출한 YearMonth 와 호환되는지 검증
        val ym = YearMonth.of(2026, 5)
        val salesYear = SalesYear.valueOf("Y${ym.year}")
        val salesMonth = SalesMonth.valueOf("M${"%02d".format(ym.monthValue)}")
        assertThat(salesYear).isEqualTo(SalesYear.Y2026)
        assertThat(salesMonth).isEqualTo(SalesMonth.M05)
        assertThat(salesYear.value).isEqualTo("2026")
        assertThat(salesMonth.value).isEqualTo("05")
    }
}
