package com.otoki.internal.service

import com.otoki.internal.dto.request.MonthlySalesRequest
import com.otoki.internal.entity.MonthlySales
import com.otoki.internal.repository.MonthlySalesRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("MonthlySalesService 테스트")
class MonthlySalesServiceTest {

    @Mock
    private lateinit var monthlySalesRepository: MonthlySalesRepository

    @InjectMocks
    private lateinit var monthlySalesService: MonthlySalesService

    // ========== getMonthlySales Tests ==========

    @Nested
    @DisplayName("getMonthlySales - 월매출 조회")
    inner class GetMonthlySalesTests {

        @Test
        @DisplayName("월매출 데이터를 정상 조회한다")
        fun getMonthlySales_success() {
            // Given
            val request = MonthlySalesRequest(
                customerId = "C001",
                yearMonth = "202602"
            )

            val currentMonthData = listOf(
                createMonthlySales(
                    customerId = "C001",
                    yearMonth = "202602",
                    category = "상온",
                    targetAmount = 43000000,
                    achievedAmount = 60050000
                ),
                createMonthlySales(
                    customerId = "C001",
                    yearMonth = "202602",
                    category = "냉장/냉동",
                    targetAmount = 15000000,
                    achievedAmount = 12000000
                )
            )

            val previousMonthData = listOf(
                createMonthlySales(
                    customerId = "C001",
                    yearMonth = "202502",
                    category = "상온",
                    achievedAmount = 25000000
                )
            )

            val currentYearData = listOf(
                createMonthlySales(yearMonth = "202601", achievedAmount = 28000000),
                createMonthlySales(yearMonth = "202602", achievedAmount = 30000000)
            )

            val previousYearData = listOf(
                createMonthlySales(yearMonth = "202501", achievedAmount = 24000000),
                createMonthlySales(yearMonth = "202502", achievedAmount = 24000000)
            )

            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202602"))
                .thenReturn(currentMonthData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202502"))
                .thenReturn(previousMonthData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202601", "202602"))
                .thenReturn(currentYearData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202501", "202502"))
                .thenReturn(previousYearData)

            // When
            val result = monthlySalesService.getMonthlySales(request)

            // Then
            assertThat(result.customerId).isEqualTo("C001")
            assertThat(result.yearMonth).isEqualTo("202602")
            assertThat(result.targetAmount).isEqualTo(58000000L)
            assertThat(result.achievedAmount).isEqualTo(72050000L)
            assertThat(result.categorySales).hasSize(2)
        }

        @Test
        @DisplayName("제품유형별 매출이 정확히 계산된다")
        fun getMonthlySales_categorySalesCalculation() {
            // Given
            val request = MonthlySalesRequest(
                customerId = "C001",
                yearMonth = "202602"
            )

            val currentMonthData = listOf(
                createMonthlySales(
                    category = "상온",
                    targetAmount = 100000,
                    achievedAmount = 150000
                ),
                createMonthlySales(
                    category = "냉장/냉동",
                    targetAmount = 50000,
                    achievedAmount = 40000
                )
            )

            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202602"))
                .thenReturn(currentMonthData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202502"))
                .thenReturn(emptyList())
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202601", "202602"))
                .thenReturn(emptyList())
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202501", "202502"))
                .thenReturn(emptyList())

            // When
            val result = monthlySalesService.getMonthlySales(request)

            // Then
            val roomTemp = result.categorySales.find { it.category == "상온" }
            assertThat(roomTemp).isNotNull
            assertThat(roomTemp!!.achievementRate).isEqualTo(150.0)

            val frozen = result.categorySales.find { it.category == "냉장/냉동" }
            assertThat(frozen).isNotNull
            assertThat(frozen!!.achievementRate).isEqualTo(80.0)
        }

        @Test
        @DisplayName("전년 동월 비교가 정확히 계산된다")
        fun getMonthlySales_yearComparison() {
            // Given
            val request = MonthlySalesRequest(
                customerId = "C001",
                yearMonth = "202602"
            )

            val currentMonthData = listOf(
                createMonthlySales(yearMonth = "202602", achievedAmount = 30000000)
            )

            val previousMonthData = listOf(
                createMonthlySales(yearMonth = "202502", achievedAmount = 25000000)
            )

            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202602"))
                .thenReturn(currentMonthData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202502"))
                .thenReturn(previousMonthData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202601", "202602"))
                .thenReturn(emptyList())
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202501", "202502"))
                .thenReturn(emptyList())

            // When
            val result = monthlySalesService.getMonthlySales(request)

            // Then
            assertThat(result.yearComparison.currentYear).isEqualTo(30000000L)
            assertThat(result.yearComparison.previousYear).isEqualTo(25000000L)
        }

        @Test
        @DisplayName("월 평균 실적이 정확히 계산된다")
        fun getMonthlySales_monthlyAverage() {
            // Given
            val request = MonthlySalesRequest(
                customerId = "C001",
                yearMonth = "202602"
            )

            val currentYearData = listOf(
                createMonthlySales(yearMonth = "202601", achievedAmount = 28000000),
                createMonthlySales(yearMonth = "202602", achievedAmount = 30000000)
            )

            val previousYearData = listOf(
                createMonthlySales(yearMonth = "202501", achievedAmount = 24000000),
                createMonthlySales(yearMonth = "202502", achievedAmount = 24000000)
            )

            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202602"))
                .thenReturn(emptyList())
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonth("C001", "202502"))
                .thenReturn(emptyList())
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202601", "202602"))
                .thenReturn(currentYearData)
            whenever(monthlySalesRepository.findByCustomerIdAndYearMonthRange("C001", "202501", "202502"))
                .thenReturn(previousYearData)

            // When
            val result = monthlySalesService.getMonthlySales(request)

            // Then: (28M + 30M) / 2 = 29M
            assertThat(result.monthlyAverage.currentYearAverage).isEqualTo(29000000L)
            // (24M + 24M) / 2 = 24M
            assertThat(result.monthlyAverage.previousYearAverage).isEqualTo(24000000L)
            assertThat(result.monthlyAverage.startMonth).isEqualTo(1)
            assertThat(result.monthlyAverage.endMonth).isEqualTo(2)
        }
    }

    // ========== Helper Functions ==========

    private fun createMonthlySales(
        id: Long = 1L,
        customerId: String = "C001",
        yearMonth: String = "202602",
        category: String = "상온",
        targetAmount: Long = 0L,
        achievedAmount: Long = 0L
    ): MonthlySales {
        return MonthlySales(
            id = id,
            customerId = customerId,
            yearMonth = yearMonth,
            category = category,
            targetAmount = targetAmount,
            achievedAmount = achievedAmount
        )
    }
}
