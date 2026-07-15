package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.OrderCancelResponse
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInFlightException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelLineNotFoundException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * 주문 취소 서비스 (Spec #597).
 *
 * 처리 흐름 (spec.md §2.1, Spec #858 흔적 기록 추가):
 *  1. 주문 + 라인 조회 + 검증 (본인 주문 / 상태 / 마감 시각 / 라인 ID)
 *  2. 취소 요청 흔적 기록 ([OrderCancelRequestRecorder]) — SAP 호출 전 `cancel_requested_at` 커밋
 *  3. SAP 동기 송신 (`OrderRequestCancelSender.send(...)`) — DB 트랜잭션 외부
 *  4. 응답 'S' 시 [OrderCancelCommitter] 위임으로 라인/헤더 상태 변경 커밋
 *
 * SAP 응답 'E' / timeout / HTML 가드 실패 시 [OrderCancelSapFailedException] (502) — `line_change_type`
 * 무변경. 단 2단계의 `cancel_requested_at` 흔적은 롤백하지 않고 유지되어(별도 트랜잭션), 이후 상세조회
 * 정합([OrderCancelReconciler])이 SAP DefaultReason 과 교집합으로 미확정 라인을 `'X'` 로 승격할 수 있다.
 *
 * 본 클래스는 의도적으로 `@Transactional` 미부여 — SAP 동기 호출을 트랜잭션 외부에서 수행해야 하므로.
 */
@Service
class OrderCancelService(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
    private val employeeRepository: EmployeeRepository,
    private val orderCancelPolicy: OrderCancelPolicy,
    private val orderRequestCancelPayloadFactory: OrderRequestCancelPayloadFactory,
    private val orderRequestCancelSender: OrderRequestCancelSender,
    private val orderCancelCommitter: OrderCancelCommitter,
    private val orderCancelRequestRecorder: OrderCancelRequestRecorder,
) {

    fun cancel(orderRequestId: Long, userId: Long, orderProductIds: List<Long>): OrderCancelResponse {
        val orderRequest = loadAndValidate(orderRequestId, userId)
        val allLines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)
        val targetLines = resolveTargetLines(allLines, orderProductIds)

        val employee = employeeRepository.findById(userId).orElseThrow { OrderNotFoundException() }
        val employeeCode = employee.employeeCode ?: error("주문 취소 요청 사원의 사번이 null - 비정상")
        val targetIds = targetLines.map { it.id }

        // SAP 호출 전 취소 요청 흔적 기록 (Spec #858) — timeout 미확정 시 상세조회 정합의 대상 식별 근거.
        orderCancelRequestRecorder.recordCancelRequested(orderRequestId, targetIds, employeeCode)

        val payload = orderRequestCancelPayloadFactory.build(orderRequest, targetLines)
        orderRequestCancelSender.send(payload)

        val result = orderCancelCommitter.commit(orderRequestId, targetIds, employeeCode)
        return OrderCancelResponse.of(result.orderRequest, result.cancelledLines)
    }

    private fun loadAndValidate(orderRequestId: Long, userId: Long): OrderRequest {
        val orderRequest = orderRequestRepository.findByIdOrNull(orderRequestId)
            ?: throw OrderNotFoundException()
        if (orderRequest.employee!!.id != userId) {
            throw ForbiddenOrderAccessException()
        }
        // 취소 가능 판정은 [OrderCancelPolicy] 단일 규칙 사용 — 상세 응답 `cancelable` 플래그와 정합.
        if (!orderCancelPolicy.isCancellableStatus(orderRequest.orderRequestStatus)) {
            throw OrderCancelInvalidStatusException(orderRequest.orderRequestStatus?.name ?: "")
        }
        // deliveryDate 는 SF nillable=true 정합으로 nullable — 마감 판단 불가(=취소 불가)로 처리.
        if (!orderCancelPolicy.isWithinCancelDeadline(orderRequest.deliveryDate)) {
            throw OrderCancelDeadlinePassedException()
        }
        // 경합 방어(Spec #597): 등록 SAP 전송이 아직 진행 중(outbox PENDING/RETRY)이면 취소 보류.
        // 등록(생성)이 SAP 에 도달하기 전 취소(삭제)가 나가는 순서 역전을 차단한다. SENT 전이 후 재시도 가능.
        if (orderCancelPolicy.isRegistrationInFlight(orderRequestId)) {
            throw OrderCancelInFlightException()
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
}
