package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ErpOrderRetentionServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val erpOrderProductRepository: ErpOrderProductRepository = mockk()
    private val service = ErpOrderRetentionService(erpOrderRepository, erpOrderProductRepository)

    @Test
    fun `6개월 경과분을 자식 라인 먼저 삭제한 뒤 헤더를 삭제한다`() {
        val expectedCutoff = LocalDate.now().minusMonths(ErpOrderRetentionService.RETENTION_MONTHS)
        every { erpOrderProductRepository.deleteByErpOrderOrderDateBefore(expectedCutoff) } returns 12
        every { erpOrderRepository.deleteByOrderDateBefore(expectedCutoff) } returns 5

        val result = service.purgeExpired()

        assertThat(result.cutoff).isEqualTo(expectedCutoff)
        assertThat(result.deletedLines).isEqualTo(12)
        assertThat(result.deletedOrders).isEqualTo(5)
        // FK 정합 — 자식(라인) 삭제가 부모(헤더) 삭제보다 먼저여야 한다.
        verifyOrder {
            erpOrderProductRepository.deleteByErpOrderOrderDateBefore(expectedCutoff)
            erpOrderRepository.deleteByOrderDateBefore(expectedCutoff)
        }
    }
}
