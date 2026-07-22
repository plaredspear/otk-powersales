package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 취소 요청 흔적 기록 컴포넌트 (Spec #858).
 *
 * 취소 SAP 호출(SD03051) **전에** 대상 라인에 `cancel_requested_at`/`cancel_requested_by` 를 기록한다.
 * SAP 미반영/실패 시에도 이 흔적을 유지하며, 상세조회에서 라인의 "취소요청" 표시 근거가 된다
 * (Spec #845 — 로컬 확정 없이 요청 흔적 vs SAP 실제(DefaultReason) 를 나란히 비교).
 *
 * 별도 빈으로 분리 — [OrderCancelService.cancel] 가 SAP 호출을 트랜잭션 외부에서 수행해야 하므로,
 * 흔적 기록만 격리된 write 트랜잭션으로 커밋한다.
 */
@Component
class OrderCancelRequestRecorder(
    private val orderRequestProductRepository: OrderRequestProductRepository,
) {

    /**
     * 취소 대상 라인에 취소 요청 흔적을 기록한다.
     *
     * 이미 확정 취소된 라인(`line_change_type='X'`)은 대상에서 이미 제외됐으므로([OrderCancelService]
     * `resolveTargetLines`), 넘어온 라인 전부에 흔적을 남긴다. SAP 응답 확정 전이므로
     * `line_change_type` 은 건드리지 않는다.
     */
    @Transactional
    fun recordCancelRequested(orderRequestId: Long, lineIds: List<Long>, employeeCode: String) {
        val targetIdSet = lineIds.toSet()
        val lines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)
            .filter { it.id in targetIdSet }
        lines.forEach { it.markCancelRequested(employeeCode) }
        log.info(
            "order.cancel.requested-recorded orderRequestId={} lineIds={}",
            orderRequestId, lines.map { it.id },
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderCancelRequestRecorder::class.java)
    }
}
