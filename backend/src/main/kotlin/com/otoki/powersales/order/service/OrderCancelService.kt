package com.otoki.powersales.order.service

import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.response.OrderCancelResponse
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.order.exception.OrderCancelLineNotFoundException
import com.otoki.powersales.order.exception.OrderNotFoundException
import com.otoki.powersales.order.repository.OrderRequestProductRepository
import com.otoki.powersales.order.repository.OrderRequestRepository
import com.otoki.powersales.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.order.util.OrderDeadlineCalculator
import com.otoki.powersales.sap.outbound.sender.OrderRequestCancelSender
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * 주문 취소 서비스 (Spec #597).
 *
 * 처리 흐름 (spec.md §2.1):
 *  1. 주문 + 라인 조회 + 검증 (본인 주문 / 상태 / 마감 시각 / 라인 ID)
 *  2. SAP 동기 송신 (`OrderRequestCancelSender.send(...)`) — DB 트랜잭션 외부
 *  3. 응답 'S' 시 [OrderCancelCommitter] 위임으로 라인/헤더 상태 변경 커밋
 *
 * SAP 응답 'E' / timeout / HTML 가드 실패 시 [OrderCancelSapFailedException] (502) — DB 무변경.
 *
 * 본 클래스는 의도적으로 `@Transactional` 미부여 — SAP 동기 호출을 트랜잭션 외부에서 수행해야 하므로.
 */
@Service
class OrderCancelService(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
    private val employeeRepository: EmployeeRepository,
    private val orderDeadlineCalculator: OrderDeadlineCalculator,
    private val orderRequestCancelPayloadFactory: OrderRequestCancelPayloadFactory,
    private val orderRequestCancelSender: OrderRequestCancelSender,
    private val orderCancelCommitter: OrderCancelCommitter,
) {

    fun cancel(orderRequestId: Long, userId: Long, orderProductIds: List<Long>): OrderCancelResponse {
        val orderRequest = loadAndValidate(orderRequestId, userId)
        val allLines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)
        val targetLines = resolveTargetLines(allLines, orderProductIds)

        val payload = orderRequestCancelPayloadFactory.build(orderRequest, targetLines)
        orderRequestCancelSender.send(payload)

        val employee = employeeRepository.findById(userId).orElseThrow { OrderNotFoundException() }
        val targetIds = targetLines.map { it.id }
        val result = orderCancelCommitter.commit(orderRequestId, targetIds, employee.employeeCode)
        return OrderCancelResponse.of(result.orderRequest, result.cancelledLines)
    }

    private fun loadAndValidate(orderRequestId: Long, userId: Long): OrderRequest {
        val orderRequest = orderRequestRepository.findByIdOrNull(orderRequestId)
            ?: throw OrderNotFoundException()
        if (orderRequest.employee.id != userId) {
            throw ForbiddenOrderAccessException()
        }
        if (orderRequest.orderRequestStatus !in CANCELLABLE_STATUSES) {
            throw OrderCancelInvalidStatusException(orderRequest.orderRequestStatus.name)
        }
        if (!orderDeadlineCalculator.isCancellable(orderRequest.deliveryDate)) {
            throw OrderCancelDeadlinePassedException()
        }
        return orderRequest
    }

    private fun resolveTargetLines(
        allLines: List<OrderRequestProduct>,
        orderProductIds: List<Long>,
    ): List<OrderRequestProduct> {
        if (orderProductIds.isEmpty()) {
            return allLines.filter { !it.isCancelled() }
        }
        val byId = allLines.associateBy { it.id }
        val invalid = orderProductIds.filter { it !in byId }
        if (invalid.isNotEmpty()) {
            throw OrderCancelLineNotFoundException(invalid)
        }
        return orderProductIds.mapNotNull { byId[it] }.filter { !it.isCancelled() }
    }

    companion object {
        /** 취소 가능 상태 (spec.md §2.1 단계 3). `DRAFT` / `CANCELED` 거부. */
        private val CANCELLABLE_STATUSES = setOf(
            OrderRequestStatus.SENT,
            OrderRequestStatus.APPROVED,
            OrderRequestStatus.SEND_FAILED,
        )
    }
}
