package com.otoki.internal.sales.service

import com.otoki.internal.sales.dto.request.MonthlySalesRequest
import com.otoki.internal.sales.repository.MonthlySalesHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * MonthlySalesService 테스트
 * TODO: V1 스키마 기반으로 Service 로직 재구현 시 테스트도 함께 재작성 (후속 스펙)
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("MonthlySalesService 테스트")
class MonthlySalesServiceTest {

    @Mock
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @InjectMocks
    private lateinit var monthlySalesService: MonthlySalesService

    @Nested
    @DisplayName("getMonthlySales - 월매출 조회 (V1 스텁)")
    inner class GetMonthlySalesTests {

        @Test
        @DisplayName("스텁 응답 - customerId와 yearMonth가 정상 반환된다")
        fun getMonthlySales_stub() {
            // Given
            val request = MonthlySalesRequest(
                customerId = "C001",
                yearMonth = "202602"
            )

            // When
            val result = monthlySalesService.getMonthlySales(request)

            // Then
            assertThat(result.customerId).isEqualTo("C001")
            assertThat(result.yearMonth).isEqualTo("202602")
        }
    }
}
