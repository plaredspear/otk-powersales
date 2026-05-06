package com.otoki.powersales.order.util

import com.otoki.powersales.common.util.TimeZones
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * 주문 취소 마감 시각 판정 (Spec #597 §2.2).
 *
 * 룰: `now + 1일 < (납기일) 13:50 KST` 통과 시 취소 가능.
 * 즉, 실질 마감 시각 = (납기일 - 1일) 13:50 KST.
 *
 * 레거시 `OrderController.java:226-291` (orderDetail) 의 `deadlineType` 룰 동등.
 */
@Component
class OrderDeadlineCalculator(
    private val clock: Clock = Clock.system(TimeZones.SEOUL_ZONE),
) {

    fun isCancellable(deliveryDate: LocalDate): Boolean {
        val nowSeoul = ZonedDateTime.now(clock).withZoneSameInstant(TimeZones.SEOUL_ZONE)
        val deadline = deliveryDate.atTime(CUTOFF_TIME).atZone(TimeZones.SEOUL_ZONE)
        return nowSeoul.plusDays(1).isBefore(deadline)
    }

    companion object {
        /** 마감 시각 13:50 KST (레거시 `dateConfirm` 13:50 룰). */
        val CUTOFF_TIME: LocalTime = LocalTime.of(13, 50)
    }
}
