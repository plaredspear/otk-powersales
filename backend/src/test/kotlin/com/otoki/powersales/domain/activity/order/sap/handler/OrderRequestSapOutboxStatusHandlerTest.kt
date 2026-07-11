package com.otoki.powersales.domain.activity.order.sap.handler

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbox.SapOutbox
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrderRequestSapOutboxStatusHandler 테스트 (#597 경합 방어)")
class OrderRequestSapOutboxStatusHandlerTest {

    private val orderRequestRepository: OrderRequestRepository = mockk()
    private val handler = OrderRequestSapOutboxStatusHandler(orderRequestRepository)

    init {
        every { orderRequestRepository.save(any<OrderRequest>()) } answers { firstArg() }
    }

    private val orderRequestId = 100L

    private fun outbox() = SapOutbox(
        id = 1L,
        domainType = SapConstants.SAP_DOMAIN_ORDER_REQUEST_REGISTER,
        aggregateId = orderRequestId,
        interfaceId = "SD03050",
        payload = "{}",
    )

    private fun order(status: OrderRequestStatus) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000001",
        orderRequestStatus = status,
    )

    @Test
    @DisplayName("성공 응답 → SENT 주문을 APPROVED 로 전이")
    fun success_transitions_to_approved() {
        val order = order(OrderRequestStatus.SENT)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(outbox(), success = true, resultMessage = "S")

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
        verify(exactly = 1) { orderRequestRepository.save(order) }
    }

    @Test
    @DisplayName("실패 응답 → SENT 주문을 SEND_FAILED 로 전이")
    fun failure_transitions_to_send_failed() {
        val order = order(OrderRequestStatus.SENT)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(outbox(), success = false, resultMessage = "E")

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.SEND_FAILED)
        verify(exactly = 1) { orderRequestRepository.save(order) }
    }

    @Test
    @DisplayName("경합 방어 - 이미 CANCELED 인 주문은 등록 응답으로 되살리지 않고 스킵")
    fun canceled_order_is_not_resurrected() {
        val order = order(OrderRequestStatus.CANCELED)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(outbox(), success = true, resultMessage = "S")

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCELED)
        verify(exactly = 0) { orderRequestRepository.save(any()) }
    }

    @Test
    @DisplayName("주문 미존재 → 예외 없이 스킵")
    fun missing_order_is_skipped() {
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns null

        handler.handle(outbox(), success = true, resultMessage = "S")

        verify(exactly = 0) { orderRequestRepository.save(any()) }
    }
}
