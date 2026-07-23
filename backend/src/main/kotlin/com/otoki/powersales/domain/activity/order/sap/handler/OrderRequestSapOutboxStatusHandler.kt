package com.otoki.powersales.domain.activity.order.sap.handler

import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.service.OrderDraftService
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
 * **APPROVED 전이 시 임시저장(draft) 삭제**: SAP 등록이 최종 성공한 시점에만 해당 사원의 `tmp_order` 를
 * 삭제한다(레거시 Heroku reqOrder 의 "등록 성공 시 tmp 삭제"에 대응하되, 삭제 시점을 접수(SENT)가 아닌
 * SAP 최종 성공으로 늦춘 정책). 이렇게 하면 `SEND_FAILED`(확정 거부/재시도 소진) 시 draft 가 보존되어,
 * "다시 재주문" 유도 시 사용자가 임시저장 내용을 복원해 재입력 부담 없이 재등록할 수 있다. draft 는
 * order_request 와 FK 로 연결되지 않고 사번당 1건이라, 삭제 대상은 "이 주문에 대응하는 draft" 가 아니라
 * "해당 사번의 현재 draft" 다(레거시 tmp_order 단일 슬롯 의미론과 동일).
 *
 * 워커 트랜잭션 외부에서 호출되므로 본 핸들러가 자체 트랜잭션 (`REQUIRES_NEW`) 으로 도메인 상태 갱신.
 */
@Component
class OrderRequestSapOutboxStatusHandler(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderDraftService: OrderDraftService,
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
            // SAP 등록 최종 성공 시점에만 해당 사원의 임시저장 삭제 (SEND_FAILED 는 draft 보존 → 재주문 복원용).
            // deleteByEmployeeId 는 멱등이라 draft 가 없어도 안전. employee 는 등록 시 항상 세팅됨.
            order.employee?.id?.let { orderDraftService.deleteByEmployeeId(it) }
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
