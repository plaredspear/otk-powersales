package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 취소 SAP timeout 미확정 라인의 상세조회 정합 컴포넌트 (Spec #858).
 *
 * 취소 SAP 호출(SD03051)이 timeout 등으로 확정되지 못해 `line_change_type='X'` 를 세팅하지 못했지만
 * SAP 는 실제로 취소를 처리한 라인을, 상세조회(SD03052) 응답의 `DefaultReason` 과 교집합으로 식별해
 * 확정 취소로 승격한다.
 *
 * 정합 조건 (라인 단위, 네 조건 모두 만족 — 부분 취소도 라인 단위라 자연 포함):
 *  1. `cancel_requested_at IS NOT NULL` — 취소 요청 흔적 존재 ([OrderCancelRequestRecorder] 기록)
 *  2. `line_change_type != 'X'` — 취소 마커 미반영 (timeout 등)
 *  3. `cancelled_at IS NULL` — 취소 확정 미완료 (2·3 AND 로 확정 라인 재정합 차단, 멱등 강화)
 *  4. SAP 응답에 해당 productCode 의 `DefaultReason` 존재 — SAP 가 취소 반영
 * (1·2·3 은 [OrderRequestProduct.isCancelReconcilable], 4 는 본 컴포넌트에서 판정)
 *
 * 취소 요청 흔적이 없는 라인(사용자가 취소 요청 안 함)은 SAP DefaultReason 이 있어도(진짜 결품 등)
 * 대상에서 제외 — 진짜 결품 오정합 원천 차단.
 *
 * 별도 빈 + `REQUIRES_NEW` 로 분리 — [OrderRequestService.getOrderRequestDetail] 이 `readOnly=true` 이므로
 * 조회 트랜잭션과 격리된 write 트랜잭션에서 정합만 커밋한다 (등록 outbox handler 의 `REQUIRES_NEW` 패턴 정합).
 */
@Component
class OrderCancelReconciler(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
) {

    /**
     * 미확정 취소 라인을 SAP DefaultReason 과 교집합으로 정합 승격한다.
     *
     * [defaultReasonProductCodes] = SAP 상세응답에서 DefaultReason 이 채워진 productCode 집합.
     * 네 조건 모두 만족한 라인을 `line_change_type='X'` + `cancelled_at`/`cancelled_by`(요청자 승계) 로 승격하고,
     * 승격 후 주문 전 라인이 취소되면 헤더 `order_request_status = CANCELED` 로 전이한다(부분 취소는 헤더 유지).
     *
     * @return 정합 승격된 productCode 집합 (호출부가 응답에 반영). 정합 대상 없으면 빈 집합.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reconcileTimedOutCancels(
        orderRequestId: Long,
        defaultReasonProductCodes: Set<String>,
    ): Set<String> {
        if (defaultReasonProductCodes.isEmpty()) return emptySet()

        val lines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)

        // 네 조건 교집합: 미확정 취소 요청(1·2·3) ∩ SAP DefaultReason 존재(4).
        val promoted = lines.filter {
            it.isCancelReconcilable() && it.productCode in defaultReasonProductCodes
        }
        if (promoted.isEmpty()) return emptySet()

        promoted.forEach { it.reconcileCancel() }

        // 승격 후 전 라인 취소 시에만 헤더 전이 (부분 취소는 헤더 유지) — OrderCancelCommitter 와 동일 규칙.
        if (lines.all { it.isCancelled() }) {
            val orderRequest = orderRequestRepository.findByIdForUpdate(orderRequestId)
            if (orderRequest != null) {
                orderRequest.orderRequestStatus = OrderRequestStatus.CANCELED
            }
        }

        val promotedCodes = promoted.mapNotNull { it.productCode }.toSet()
        log.info(
            "order.cancel.reconciled orderRequestId={} promotedProductCodes={} promotedLineIds={}",
            orderRequestId, promotedCodes, promoted.map { it.id },
        )
        return promotedCodes
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderCancelReconciler::class.java)
    }
}
