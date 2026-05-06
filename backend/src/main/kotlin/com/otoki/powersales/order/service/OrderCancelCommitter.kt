package com.otoki.powersales.order.service

import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.exception.OrderNotFoundException
import com.otoki.powersales.order.repository.OrderRequestProductRepository
import com.otoki.powersales.order.repository.OrderRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 취소 상태 전이 커미터 (Spec #597).
 *
 * SAP 응답 `'S'` 수신 후 호출 — 단일 DB 트랜잭션으로 라인/헤더 상태 변경 커밋.
 * 별도 빈으로 분리 — `OrderCancelService.cancel()` 가 SAP 호출을 트랜잭션 외부에서 수행해야 하므로,
 * 자기 호출(`this.commit(...)`) 의 트랜잭션 프록시 미적용 문제를 회피한다.
 */
@Component
class OrderCancelCommitter(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
) {

    /** @return (refreshed orderRequest, cancelledLines) — 응답 매핑용 */
    @Transactional
    fun commit(orderRequestId: Long, lineIds: List<Long>, employeeCode: String): CommitResult {
        val orderRequest = orderRequestRepository.findByIdOrNull(orderRequestId)
            ?: throw OrderNotFoundException()
        val lines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)
        val targetIdSet = lineIds.toSet()
        val newlyCancelled = lines.filter { it.id in targetIdSet && !it.isCancelled }
        newlyCancelled.forEach { it.cancel(employeeCode) }

        if (lines.all { it.isCancelled }) {
            orderRequest.orderRequestStatus = OrderRequestStatus.CANCELED
        }
        log.info(
            "order.cancel.committed orderRequestId={} cancelledLineIds={} headerStatus={}",
            orderRequestId, newlyCancelled.map { it.id }, orderRequest.orderRequestStatus,
        )
        return CommitResult(orderRequest, newlyCancelled)
    }

    data class CommitResult(
        val orderRequest: OrderRequest,
        val cancelledLines: List<OrderRequestProduct>,
    )

    companion object {
        private val log = LoggerFactory.getLogger(OrderCancelCommitter::class.java)
    }
}
