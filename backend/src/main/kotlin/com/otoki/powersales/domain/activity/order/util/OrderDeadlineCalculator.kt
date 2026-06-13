package com.otoki.powersales.domain.activity.order.util

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 주문 등록/취소 공통 마감 시각 판정.
 *
 * 룰: `now + 1일 < (납기일) 13:50 KST` 통과 시 마감 전.
 * 즉, 실질 마감 시각 = (납기일 - 1일) 13:50 KST.
 *
 * 레거시 동등:
 * - 등록: `OrderController.java:475-498` (reqOrder `dateConfirm`) — `compareTime(now+1일) < deadTime(납기일 13:50)`.
 * - 취소: `OrderController.java:226-291` (orderDetail) 의 `deadlineType` 룰.
 */
@Component
class OrderDeadlineCalculator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * 주문 등록/취소 마감 전 여부.
     *
     * @return `true` = 마감 전 (등록/취소 가능), `false` = 마감 후.
     */
    fun isWithinDeadline(deliveryDate: LocalDate): Boolean {
        val now = LocalDateTime.now(clock)
        val deadline = deliveryDate.atTime(CUTOFF_TIME)
        return now.plusDays(1).isBefore(deadline)
    }

    /** 취소 가능 여부 (마감 전 판정 위임). */
    fun isCancellable(deliveryDate: LocalDate): Boolean = isWithinDeadline(deliveryDate)

    companion object {
        /** 마감 시각 13:50 KST (레거시 `dateConfirm` 13:50 룰). */
        val CUTOFF_TIME: LocalTime = LocalTime.of(13, 50)
    }
}
