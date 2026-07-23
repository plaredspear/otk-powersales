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

    // 워커(SapOutboxBatchService.processOne)가 handle 호출 전에 outbox.status 를 확정한다:
    //  - 성공 → SENT / 최종 실패(재시도 초과) → FAILED / 재시도 대기 → RETRY.
    // 핸들러는 이 status 로 "최종 실패" 와 "재시도 중" 을 구분하므로, 테스트도 실제 status 를 넘겨 검증.
    private fun outbox(status: String = SapOutbox.STATUS_SENT) = SapOutbox(
        id = 1L,
        domainType = SapConstants.SAP_DOMAIN_ORDER_REQUEST_REGISTER,
        aggregateId = orderRequestId,
        interfaceId = "SD03050",
        payload = "{}",
        status = status,
    )

    private fun order(status: OrderRequestStatus) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000001",
        orderRequestStatus = status,
    )

    @Test
    @DisplayName("성공 응답 → SENT 주문을 APPROVED 로 전이 + 이전 실패 사유 정리")
    fun success_transitions_to_approved() {
        val order = order(OrderRequestStatus.SENT).apply { sendFailReason = "이전 실패 사유" }
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(outbox(), success = true, resultMessage = "S", rejected = false)

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
        // 성공 전이 시 이전 실패 사유는 정리된다.
        assertThat(order.sendFailReason).isNull()
        verify(exactly = 1) { orderRequestRepository.save(order) }
    }

    @Test
    @DisplayName("SAP 명시적 거부(rejected) → SEND_FAILED + SAP 사유 원문 기록")
    fun rejected_transitions_to_send_failed_with_reason() {
        val order = order(OrderRequestStatus.SENT)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(
            outbox(SapOutbox.STATUS_FAILED),
            success = false,
            resultMessage = "여신 한도 초과",
            rejected = true,
        )

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.SEND_FAILED)
        // 확정 거부이므로 SAP 사유 원문을 노출용으로 기록.
        assertThat(order.sendFailReason).isEqualTo("여신 한도 초과")
        verify(exactly = 1) { orderRequestRepository.save(order) }
    }

    @Test
    @DisplayName("일시 장애 재시도 소진(outbox FAILED, rejected=false) → SEND_FAILED + 사유 null")
    fun transient_final_failure_transitions_without_reason() {
        val order = order(OrderRequestStatus.SENT)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(
            outbox(SapOutbox.STATUS_FAILED),
            success = false,
            resultMessage = "HTTP_500",
            rejected = false,
        )

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.SEND_FAILED)
        // 코드성 실패 메시지는 사용자 대상 사유가 아니므로 노출하지 않는다.
        assertThat(order.sendFailReason).isNull()
        verify(exactly = 1) { orderRequestRepository.save(order) }
    }

    @Test
    @DisplayName("재시도 대기(outbox RETRY) 실패 → SEND_FAILED 로 내리지 않고 SENT 유지 + save 미호출")
    fun retry_pending_failure_keeps_sent() {
        val order = order(OrderRequestStatus.SENT)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(
            outbox(SapOutbox.STATUS_RETRY),
            success = false,
            resultMessage = "NETWORK_ERROR",
            rejected = false,
        )

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.SENT)
        verify(exactly = 0) { orderRequestRepository.save(any()) }
    }

    @Test
    @DisplayName("경합 방어 - 이미 CANCEL_REQUESTED 인 주문은 등록 응답으로 되살리지 않고 스킵")
    fun canceled_order_is_not_resurrected() {
        val order = order(OrderRequestStatus.CANCEL_REQUESTED)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        handler.handle(outbox(), success = true, resultMessage = "S", rejected = false)

        assertThat(order.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED)
        verify(exactly = 0) { orderRequestRepository.save(any()) }
    }

    @Test
    @DisplayName("주문 미존재 → 예외 없이 스킵")
    fun missing_order_is_skipped() {
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns null

        handler.handle(outbox(), success = true, resultMessage = "S", rejected = false)

        verify(exactly = 0) { orderRequestRepository.save(any()) }
    }
}
