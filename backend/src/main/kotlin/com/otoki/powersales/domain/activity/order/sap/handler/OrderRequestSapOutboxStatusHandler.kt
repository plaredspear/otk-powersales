package com.otoki.powersales.domain.activity.order.sap.handler

import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbox.SapOutbox
import com.otoki.powersales.external.sap.outbox.SapOutboxStatusHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 등록 outbox 송신 결과를 [com.otoki.powersales.domain.activity.order.entity.OrderRequest.orderRequestStatus] 에 반영 (Spec #592).
 *
 * 상태 전이:
 *  - SAP `resultCode='S'` → `APPROVED`
 *  - SAP 명시적 거부(`rejected=true`, outbox `FAILED`) → `SEND_FAILED` + SAP 사유 원문 기록.
 *  - SAP 송신 실패이고 재시도 한도 초과(outbox `FAILED`) → `SEND_FAILED` (사유 원문 없음 → null).
 *  - SAP 송신 실패이나 재시도 대기(outbox `RETRY`/`PENDING`, in-flight) → **상태 미변경(SENT 유지)**.
 *    재시도 진행 중을 최종 "전송실패" 로 오표시하지 않기 위함이다. SENT 로 남겨두면 목록/상세의
 *    과도상태 자동 폴링 대상으로 유지되어, 이후 재시도가 성공하면 APPROVED 로 자연스럽게 전이한다.
 *
 * `SEND_FAILED` 전이 시 SAP 업무 거부 사유(`resutlMsg` 원문)를 `order_request.send_fail_reason` 에 기록해
 * 사용자가 상세에서 실패 사유를 확인할 수 있게 한다(비동기라 등록 응답 시점엔 노출 불가). 성공(APPROVED)
 * 전이 시엔 이전 실패 사유를 정리(null)한다.
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
    override fun handle(outbox: SapOutbox, success: Boolean, resultMessage: String?, rejected: Boolean) {
        // 취소 커밋과 직렬화 — 동일 행에 PESSIMISTIC_WRITE 락 획득 후 상태 판정/갱신.
        val order = orderRequestRepository.findByIdForUpdate(outbox.aggregateId)
        if (order == null) {
            log.warn("OrderRequest 미존재 outboxId={} aggregateId={}", outbox.id, outbox.aggregateId)
            return
        }
        // 경합 방어: 이미 취소(CANCEL_REQUESTED, 터미널)된 주문을 등록 응답으로 되살리지 않는다.
        if (order.orderRequestStatus == OrderRequestStatus.CANCEL_REQUESTED) {
            log.info(
                "OrderRequest 이미 취소됨 - 등록 응답 상태전이 스킵 orderRequestId={} success={}",
                order.id, success
            )
            return
        }
        // 재시도 대기(outbox in-flight) 중인 실패는 아직 "최종 실패" 가 아니므로 SEND_FAILED 로 내리지 않고
        // SENT(전송 중) 를 유지한다 — 이후 재시도 성공 시 APPROVED 로 전이. 최종 실패(outbox FAILED)만 SEND_FAILED.
        if (!success && outbox.status in SapOutbox.IN_FLIGHT_STATUSES) {
            log.info(
                "OrderRequest 등록 SAP 재시도 대기 - SEND_FAILED 전이 보류(SENT 유지) orderRequestId={} outboxStatus={} resultMsg={}",
                order.id, outbox.status, resultMessage
            )
            return
        }
        if (success) {
            order.orderRequestStatus = OrderRequestStatus.APPROVED
            // 성공 전이 시 이전 실패 사유 정리 (재전송 성공 등).
            order.sendFailReason = null
        } else {
            order.orderRequestStatus = OrderRequestStatus.SEND_FAILED
            // SAP 명시적 거부(rejected)일 때만 SAP 사유 원문을 노출용으로 기록. 재시도 소진/설정 오류 등
            // 코드성 실패 메시지(HTTP_5xx, NETWORK_ERROR 등)는 사용자 대상 사유가 아니므로 null 로 남긴다.
            order.sendFailReason = if (rejected) resultMessage else null
        }
        orderRequestRepository.save(order)
        log.info(
            "OrderRequest 상태 전이 orderRequestId={} success={} rejected={} status={} resultMsg={}",
            order.id, success, rejected, order.orderRequestStatus, resultMessage
        )
    }
}
