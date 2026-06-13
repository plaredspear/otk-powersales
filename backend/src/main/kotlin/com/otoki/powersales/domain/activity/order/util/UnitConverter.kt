package com.otoki.powersales.domain.activity.order.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 단위 환산 검증 유틸 (Spec #592 §2.2).
 *
 * 레거시 정합: `OrderController.java:548-562, 644-645` —
 *  - `unit == 'EA'` 면 환산 없이 quantity 그대로 quantityPieces
 *  - 그 외 단위면 `quantityPieces == quantity * conversionQuantity`
 */
object UnitConverter {

    private const val UNIT_EA = "EA"

    /**
     * 단위 환산 정합 검증.
     *
     * @return true 면 정합, false 면 위반 (`ORD_INVALID_UNIT`)
     */
    fun isPiecesValid(
        quantity: BigDecimal,
        unit: String,
        quantityPieces: Int,
        conversionQuantity: Int,
    ): Boolean {
        if (unit == UNIT_EA) {
            return quantity.setScale(0, RoundingMode.HALF_UP).toInt() == quantityPieces
        }
        val expected = quantity.multiply(BigDecimal.valueOf(conversionQuantity.toLong()))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        return expected == quantityPieces
    }
}
