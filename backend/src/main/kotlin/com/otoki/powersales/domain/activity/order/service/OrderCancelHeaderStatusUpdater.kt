package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 취소 헤더 상태 전이 컴포넌트.
 *
 * SAP 취소 응답 `'S'` 수신 후 호출 — 전량취소(전 라인이 취소요청 흔적 or 확정 취소로 커버)면 헤더
 * [OrderRequest.orderRequestStatus] 를 [OrderRequestStatus.CANCEL_REQUESTED] 로 전이한다. **헤더 전이만**
 * 수행하며 라인 `line_change_type='X'` 는 절대 세팅하지 않는다 — 취소요청 흔적(cancel_requested_at) vs
 * SAP 실제(DefaultReason) 비교 모델을 헤더 전이가 훼손하지 않도록 라인은 불변으로 둔다(Spec #845).
 *
 * 별도 빈으로 분리 — [OrderCancelService.cancel] 가 SAP 호출을 트랜잭션 외부에서 수행하므로 자기 호출
 * 프록시 미적용 문제를 회피하고, 헤더 전이만 격리된 write 트랜잭션으로 커밋한다.
 *
 * 등록 outbox 워커([com.otoki.powersales.domain.activity.order.sap.handler.OrderRequestSapOutboxStatusHandler])
 * 의 헤더 상태 전이와 직렬화하기 위해 동일 행에 `findByIdForUpdate`(PESSIMISTIC_WRITE) 락을 획득한다.
 */
@Component
class OrderCancelHeaderStatusUpdater(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
) {

    /**
     * 전량취소 커버 시 헤더를 [OrderRequestStatus.CANCEL_REQUESTED] 로 전이한다.
     *
     * 전량 판정: 라인이 1개 이상이고 모든 라인이 `cancel_requested_at != null` 또는 확정 취소(`isCancelled()`)
     * 이면 전량 커버로 본다(누적 취소 대응 — 일부 라인이 이미 취소요청된 상태에서 나머지를 취소하면 전량 커버).
     * 이미 CANCEL_REQUESTED 면 멱등하게 그대로 두고, 부분 커버(미달)면 상태를 변경하지 않는다.
     *
     * @return 갱신(또는 무변경)된 [OrderRequest]
     */
    @Transactional
    fun markCancelRequestedIfFullyCovered(orderRequestId: Long): OrderRequest {
        // 등록 outbox 워커의 헤더 전이와 직렬화 — 동일 행에 PESSIMISTIC_WRITE 락 획득 후 판정/갱신.
        val orderRequest = orderRequestRepository.findByIdForUpdate(orderRequestId)
            ?: throw OrderNotFoundException()

        if (orderRequest.orderRequestStatus == OrderRequestStatus.CANCEL_REQUESTED) {
            log.info("order.cancel.header 이미 CANCEL_REQUESTED - 멱등 스킵 orderRequestId={}", orderRequestId)
            return orderRequest
        }

        val lines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)

        val fullyCovered = lines.isNotEmpty() &&
            lines.all { it.cancelRequestedAt != null || it.isCancelled() }

        if (fullyCovered) {
            orderRequest.orderRequestStatus = OrderRequestStatus.CANCEL_REQUESTED
            log.info(
                "order.cancel.header 전량취소 커버 - CANCEL_REQUESTED 전이 orderRequestId={} lineCount={}",
                orderRequestId, lines.size,
            )
        } else {
            log.info(
                "order.cancel.header 부분취소(커버 미달) - 헤더 무변경 orderRequestId={} status={} lineCount={}",
                orderRequestId, orderRequest.orderRequestStatus, lines.size,
            )
        }
        return orderRequest
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderCancelHeaderStatusUpdater::class.java)
    }
}
