package com.otoki.powersales.order.sap.handler

import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.repository.OrderRequestRepository
import com.otoki.powersales.sap.SapConstants
import com.otoki.powersales.sap.outbox.SapOutbox
import com.otoki.powersales.sap.outbox.SapOutboxStatusHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 등록 outbox 송신 결과를 [com.otoki.powersales.order.entity.OrderRequest.orderRequestStatus] 에 반영 (Spec #592).
 *
 * 상태 전이:
 *  - SAP `resultCode='S'` → `APPROVED`
 *  - SAP `resultCode='E'` 또는 HTML 응답 또는 timeout → `SEND_FAILED`
 *
 * 워커 트랜잭션 외부에서 호출되므로 본 핸들러가 자체 트랜잭션 (`REQUIRES_NEW`) 으로 도메인 상태 갱신.
 */
@Component
class OrderRequestSapOutboxStatusHandler(
    private val orderRequestRepository: OrderRequestRepository,
) : SapOutboxStatusHandler {

    private val log = LoggerFactory.getLogger(OrderRequestSapOutboxStatusHandler::class.java)

    override fun supports(): String = SapConstants.SAP_DOMAIN_ORDER_REQUEST_REGISTER

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun handle(outbox: SapOutbox, success: Boolean, resultMessage: String?) {
        val order = orderRequestRepository.findById(outbox.aggregateId).orElse(null)
        if (order == null) {
            log.warn("OrderRequest 미존재 outboxId={} aggregateId={}", outbox.id, outbox.aggregateId)
            return
        }
        order.orderRequestStatus = if (success) OrderRequestStatus.APPROVED else OrderRequestStatus.SEND_FAILED
        orderRequestRepository.save(order)
        log.info(
            "OrderRequest 상태 전이 orderRequestId={} success={} status={} resultMsg={}",
            order.id, success, order.orderRequestStatus, resultMessage
        )
    }
}
