package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbox.SapOutbox
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 주문 취소 가능 판정 단일 정책 (Spec #597).
 *
 * 취소 엔드포인트([OrderCancelService]) 의 서버 가드와, 상세 응답
 * ([com.otoki.powersales.domain.activity.order.dto.response.OrderRequestDetailResponse]) 의
 * `cancelable` 플래그가 **동일 규칙**을 공유하도록 중앙화한다 — "버튼은 떴는데 409" 불일치 방지.
 *
 * 취소 가능 = 취소가능 상태 && 마감 전 && 등록 SAP 전송 not in-flight.
 */
@Component
class OrderCancelPolicy(
    private val orderDeadlineCalculator: OrderDeadlineCalculator,
    private val sapOutboxRepository: SapOutboxRepository,
) {
    /** 취소 가능 상태(SENT/APPROVED/SEND_FAILED) 여부. */
    fun isCancellableStatus(status: OrderRequestStatus?): Boolean =
        status in OrderRequestStatus.CANCELLABLE

    /** 마감 전(취소 가능 시각) 여부. `deliveryDate` null 은 마감 판단 불가 → 취소 불가. */
    fun isWithinCancelDeadline(deliveryDate: LocalDate?): Boolean =
        deliveryDate != null && orderDeadlineCalculator.isCancellable(deliveryDate)

    /** 등록 SAP 전송이 아직 진행 중(outbox PENDING/RETRY)인지 여부. */
    fun isRegistrationInFlight(orderRequestId: Long): Boolean =
        sapOutboxRepository.existsByDomainTypeAndAggregateIdAndStatusIn(
            SapConstants.SAP_DOMAIN_ORDER_REQUEST_REGISTER,
            orderRequestId,
            SapOutbox.IN_FLIGHT_STATUSES,
        )

    /** 취소 가능 여부 종합 판정 (상세 응답 `cancelable` 플래그용). */
    fun isCancelable(orderRequest: OrderRequest): Boolean =
        isCancellableStatus(orderRequest.orderRequestStatus) &&
            isWithinCancelDeadline(orderRequest.deliveryDate) &&
            !isRegistrationInFlight(orderRequest.id)
}
