package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderAlreadyClosedException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 재전송 서비스 (Spec F18 / #595 후속).
 *
 * 전송실패(`SEND_FAILED`) 상태의 주문을 SAP 로 재전송한다. 등록(#592)과 동일한 비동기 outbox 경로를 재사용한다:
 *  1. 검증: 본인 주문 / `SEND_FAILED` 상태 / 마감 전.
 *  2. 헤더 상태를 `SENT` 로 되돌리고, `sap_outbox` 에 재등록 row 적재([OrderRequestRegisterSender.enqueue]).
 *  3. 이후 [com.otoki.powersales.external.sap.outbox.SapOutboxBatchService] 워커가 송신하고
 *     [com.otoki.powersales.domain.activity.order.sap.handler.OrderRequestSapOutboxStatusHandler] 가
 *     결과에 따라 `APPROVED` / `SEND_FAILED` 로 다시 전이한다.
 *
 * 별도 `RESEND` 상태는 두지 않는다 — 재전송 = 등록 재시도이므로 `SENT` 로 복귀하면 충분하고,
 * 모바일 재전송 버튼은 `SEND_FAILED` 일 때만 노출되므로 `SENT` 복귀로 자연히 숨겨진다.
 *
 * enqueue 는 도메인 트랜잭션 내에서 outbox 적재만 하므로 (sap-integration.md §11 sender 컨벤션)
 * 본 서비스는 등록과 동일하게 `@Transactional` 단일 일관성을 유지한다.
 */
@Service
class OrderRequestResendService(
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
    private val orderDeadlineCalculator: OrderDeadlineCalculator,
    private val orderRequestRegisterSender: OrderRequestRegisterSender,
    private val orderRegistrationBlockGuard: OrderRegistrationBlockGuard,
) {

    @Transactional
    fun resend(orderRequestId: Long, userId: Long) {
        // 운영(prod) 환경 임시 차단 — 등록과 동일하게 재전송도 막는다
        orderRegistrationBlockGuard.assertNotBlocked()

        val orderRequest = loadAndValidate(orderRequestId, userId)
        val lines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId)

        orderRequest.orderRequestStatus = OrderRequestStatus.SENT
        orderRequestRepository.save(orderRequest)
        orderRequestRegisterSender.enqueue(orderRequest, lines)
    }

    private fun loadAndValidate(orderRequestId: Long, userId: Long): OrderRequest {
        val orderRequest = orderRequestRepository.findByIdOrNull(orderRequestId)
            ?: throw OrderNotFoundException()
        if (orderRequest.employee!!.id != userId) {
            throw ForbiddenOrderAccessException()
        }
        if (orderRequest.orderRequestStatus != OrderRequestStatus.SEND_FAILED) {
            throw InvalidOrderStatusException()
        }
        // deliveryDate 는 SF nillable=true 정합으로 nullable — 마감 판단 불가(=마감)로 처리.
        val deliveryDate = orderRequest.deliveryDate
            ?: throw OrderAlreadyClosedException("마감된 주문은 재전송할 수 없습니다")
        if (!orderDeadlineCalculator.isWithinDeadline(deliveryDate)) {
            throw OrderAlreadyClosedException("마감된 주문은 재전송할 수 없습니다")
        }
        return orderRequest
    }
}
