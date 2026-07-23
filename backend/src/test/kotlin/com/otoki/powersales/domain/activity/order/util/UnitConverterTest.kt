package com.otoki.powersales.domain.activity.order.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("UnitConverter 단위 환산")
class UnitConverterTest {

    @Nested
    @DisplayName("toDisplayBoxQuantity — 표시용 박스 (레거시 CRM_TotalQuantity_Box = 총EA ÷ 박스입수)")
    inner class ToDisplayBoxQuantity {

        @Test
        @DisplayName("박스 미달 낱개 → 소수 박스 (계란 10구: 10개 ÷ 박스입수 20 = 0.5)")
        fun fractionalBox() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = BigDecimal("10"),
                boxReceivingQuantity = BigDecimal("20"),
                storedBoxes = BigDecimal("1.00"), // SAP 환산수량(10) 기반 저장값 — 표시엔 쓰지 않음
            )
            assertThat(result).isEqualByComparingTo("0.5")
        }

        @Test
        @DisplayName("정수 박스 (12개 ÷ 박스입수 12 = 1)")
        fun wholeBox() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = BigDecimal("12"),
                boxReceivingQuantity = BigDecimal("12"),
                storedBoxes = BigDecimal("1.00"),
            )
            assertThat(result).isEqualByComparingTo("1")
        }

        @Test
        @DisplayName("소수 2자리 반올림 (레거시 toFixed(2): 3개 ÷ 8 = 0.38)")
        fun roundsToTwoScale() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = BigDecimal("3"),
                boxReceivingQuantity = BigDecimal("8"),
                storedBoxes = BigDecimal.ZERO,
            )
            assertThat(result).isEqualByComparingTo("0.38")
        }

        @Test
        @DisplayName("박스입수 null → 저장값 폴백")
        fun nullBoxReceivingFallsBack() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = BigDecimal("10"),
                boxReceivingQuantity = null,
                storedBoxes = BigDecimal("7"),
            )
            assertThat(result).isEqualByComparingTo("7")
        }

        @Test
        @DisplayName("박스입수 0 → 저장값 폴백 (0 나눗셈 방지)")
        fun zeroBoxReceivingFallsBack() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = BigDecimal("10"),
                boxReceivingQuantity = BigDecimal.ZERO,
                storedBoxes = BigDecimal("7"),
            )
            assertThat(result).isEqualByComparingTo("7")
        }

        @Test
        @DisplayName("총EA null → 저장값 폴백")
        fun nullPiecesFallsBack() {
            val result = UnitConverter.toDisplayBoxQuantity(
                quantityPieces = null,
                boxReceivingQuantity = BigDecimal("20"),
                storedBoxes = BigDecimal("2"),
            )
            assertThat(result).isEqualByComparingTo("2")
        }
    }
}
