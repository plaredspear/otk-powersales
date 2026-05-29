package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.dto.request.MonthlySalesRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MonthlySalesService 테스트")
class MonthlySalesServiceTest {

    private val ororaGateway: OroraMonthlySalesHistoryQueryGateway = mockk()
    private val service = MonthlySalesService(ororaGateway)

    @Nested
    @DisplayName("getMonthlySales — ORORA 기반 응답")
    inner class GetMonthlySalesTests {

        @Test
        @DisplayName("customerId / yearMonth 가 응답에 그대로 전달된다")
        fun returnsRequestEcho() {
            every { ororaGateway.findBySalesDates(any(), any()) } returns emptyList()

            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "C001", yearMonth = "202602")
            )

            assertThat(result.customerId).isEqualTo("C001")
            assertThat(result.yearMonth).isEqualTo("202602")
            assertThat(result.achievedAmount).isEqualTo(0L)
            assertThat(result.targetAmount).isEqualTo(0L)
        }

        @Test
        @DisplayName("customerId 가 null 이면 ALL 로 응답 + ORORA 호출 안 함")
        fun nullCustomerIdReturnsAll() {
            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = null, yearMonth = "202602")
            )

            assertThat(result.customerId).isEqualTo("ALL")
            assertThat(result.achievedAmount).isEqualTo(0L)
        }
    }
}
