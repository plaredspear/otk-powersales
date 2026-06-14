package com.otoki.powersales.domain.activity.order.util

import com.otoki.powersales.platform.common.util.TimeZones
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("OrderDeadlineCalculator 테스트 (#597)")
class OrderDeadlineCalculatorTest {

    private val deliveryDate = LocalDate.of(2026, 5, 4)

    @Test
    @DisplayName("성공 — 마감 시각(납기일 -1일 13:50 KST) 이전이면 취소 가능")
    fun cancellableBeforeDeadline() {
        // 2026-05-03 13:49 KST → cancellable
        val calc = calculatorAt("2026-05-03T13:49:00")
        assertThat(calc.isCancellable(deliveryDate)).isTrue()
    }

    @Test
    @DisplayName("실패 — 마감 시각 정각(납기일 -1일 13:50 KST) 도달 시 취소 불가")
    fun notCancellableAtDeadline() {
        // 2026-05-03 13:50 KST → NOT cancellable (now+1day == deadline, not isBefore)
        val calc = calculatorAt("2026-05-03T13:50:00")
        assertThat(calc.isCancellable(deliveryDate)).isFalse()
    }

    @Test
    @DisplayName("실패 — 마감 시각 이후 취소 불가")
    fun notCancellableAfterDeadline() {
        // 2026-05-03 13:51 KST → NOT cancellable
        val calc = calculatorAt("2026-05-03T13:51:00")
        assertThat(calc.isCancellable(deliveryDate)).isFalse()
    }

    @Test
    @DisplayName("실패 — 납기일 당일은 항상 취소 불가")
    fun notCancellableOnDeliveryDate() {
        val calc = calculatorAt("2026-05-04T08:00:00")
        assertThat(calc.isCancellable(deliveryDate)).isFalse()
    }

    @Test
    @DisplayName("성공 — 충분히 이른 시각이면 취소 가능 (이틀 전 자정)")
    fun cancellableEarly() {
        val calc = calculatorAt("2026-05-02T00:00:00")
        assertThat(calc.isCancellable(deliveryDate)).isTrue()
    }

    private fun calculatorAt(seoulIso: String): OrderDeadlineCalculator {
        val instant = LocalDateTime.parse(seoulIso).atZone(TimeZones.SEOUL_ZONE).toInstant()
        return OrderDeadlineCalculator(Clock.fixed(instant, TimeZones.SEOUL_ZONE))
    }
}
