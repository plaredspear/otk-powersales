package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * [MonthlySalesHistoryQueryGateway] 동작 검증.
 *
 * - YYYYMM → (SalesYear, SalesMonth) 변환 + 정확한 (년, 월) 쌍 매칭
 * - closingAmountSum = (abc1~4) + (ship1~4) 재합산 (formula 복제 컬럼 미사용)
 * - isDeleted soft-delete row 제외
 * - 빈 입력 → repository 호출 skip
 */
@DisplayName("MonthlySalesHistoryQueryGateway 동작 검증")
class MonthlySalesHistoryQueryGatewayTest {

    private val repository: MonthlySalesHistoryRepository = mockk()
    private val gateway = MonthlySalesHistoryQueryGateway(repository)

    private fun row(
        sap: String,
        year: String,
        month: String,
        abc1: Double? = null,
        abc2: Double? = null,
        abc3: Double? = null,
        abc4: Double? = null,
        ship1: Double? = null,
        ship2: Double? = null,
        ship3: Double? = null,
        ship4: Double? = null,
        isDeleted: Boolean? = false,
    ) = MonthlySalesHistory(
        sapAccountCode = sap,
        salesYear = SalesYear.fromValueOrNull(year),
        salesMonth = SalesMonth.fromValueOrNull(month),
        abcClosingAmount1 = abc1,
        abcClosingAmount2 = abc2,
        abcClosingAmount3 = abc3,
        abcClosingAmount4 = abc4,
        shipClosingAmount1 = ship1,
        shipClosingAmount2 = ship2,
        shipClosingAmount3 = ship3,
        shipClosingAmount4 = ship4,
        isDeleted = isDeleted,
    )

    @Test
    @DisplayName("closingAmountSum = (abc1~4) + (ship1~4) 재합산 / null 컬럼은 0 치환")
    fun closingAmountSumRecomputed() {
        every {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns listOf(
            row("SAP1", "2026", "05", abc1 = 100.0, abc3 = 200.0, ship1 = 50.0, ship4 = 150.0),
        )

        val result = gateway.findBySalesDates(listOf("202605"), listOf("SAP1"))

        assertThat(result).hasSize(1)
        // (100 + 0 + 200 + 0) + (50 + 0 + 0 + 150) = 500
        assertThat(result.first().closingAmountSum).isEqualByComparingTo(BigDecimal("500"))
        assertThat(result.first().abcClosingAmount1).isEqualByComparingTo(BigDecimal("100"))
    }

    @Test
    @DisplayName("salesDate(YYYYMM) 복원 + shipClosingAmount1~3 개별 노출 (물류매출 온도대별)")
    fun salesDateAndShipColumnsExposed() {
        every {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns listOf(
            row("SAP1", "2026", "05", ship1 = 10.0, ship2 = 20.0, ship3 = 30.0, ship4 = 40.0),
        )

        val result = gateway.findBySalesDates(listOf("202605"), listOf("SAP1"))

        assertThat(result).hasSize(1)
        val r = result.first()
        assertThat(r.salesDate).isEqualTo("202605")
        assertThat(r.shipClosingAmount1).isEqualByComparingTo(BigDecimal("10"))
        assertThat(r.shipClosingAmount2).isEqualByComparingTo(BigDecimal("20"))
        assertThat(r.shipClosingAmount3).isEqualByComparingTo(BigDecimal("30"))
        // ship4(유지) 는 row 에 노출하지 않음 (물류매출 화면 온도대 3종 범위)
    }

    @Test
    @DisplayName("요청한 (년, 월) 쌍 밖의 cartesian 후보 row 는 제외")
    fun filtersCartesianNonMatchingPairs() {
        // 요청: 202605, 202604. Repository 는 (2026 IN) × (05, 04 IN) cartesian 후보를 반환할 수 있음.
        every {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns listOf(
            row("SAP1", "2026", "05", abc1 = 100.0),  // 매칭
            row("SAP1", "2026", "04", abc1 = 200.0),  // 매칭
            row("SAP1", "2026", "03", abc1 = 999.0),  // 요청 밖 → 제외
        )

        val result = gateway.findBySalesDates(listOf("202605", "202604"), listOf("SAP1"))

        assertThat(result).hasSize(2)
        assertThat(result.map { it.closingAmountSum })
            .usingElementComparator(Comparator { a, b -> a.compareTo(b) })
            .containsExactlyInAnyOrder(BigDecimal("100"), BigDecimal("200"))
    }

    @Test
    @DisplayName("isDeleted=true row 제외")
    fun excludesSoftDeleted() {
        every {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns listOf(
            row("SAP1", "2026", "05", abc1 = 100.0, isDeleted = false),
            row("SAP1", "2026", "05", abc1 = 999.0, isDeleted = true),
        )

        val result = gateway.findBySalesDates(listOf("202605"), listOf("SAP1"))

        assertThat(result).hasSize(1)
        assertThat(result.first().closingAmountSum).isEqualByComparingTo(BigDecimal("100"))
    }

    @Test
    @DisplayName("빈 sapAccountCodes 또는 빈 salesDates → repository 호출 skip")
    fun emptyInputSkip() {
        val r1 = gateway.findBySalesDates(listOf("202605"), emptyList())
        val r2 = gateway.findBySalesDates(emptyList(), listOf("SAP1"))

        assertThat(r1).isEmpty()
        assertThat(r2).isEmpty()
        verify(exactly = 0) {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        }
    }

    @Test
    @DisplayName("picklist 범위 밖 YYYYMM → 변환 불가로 빈 결과 + repository 호출 skip")
    fun unparseableYearMonthSkip() {
        val result = gateway.findBySalesDates(listOf("209905"), listOf("SAP1"))

        assertThat(result).isEmpty()
        verify(exactly = 0) {
            repository.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        }
    }
}
