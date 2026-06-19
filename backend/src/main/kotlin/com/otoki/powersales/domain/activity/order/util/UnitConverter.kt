package com.otoki.powersales.domain.activity.order.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 단위 환산 검증/변환 유틸 (Spec #592 §2.2).
 *
 * 레거시 정합: `OrderController.java:630-644` —
 *  - `unit == 'EA'` 면 환산 없이 총 EA(`quantityPieces`) 그대로 (배수 검증 없음)
 *  - 그 외 단위(BOX 등)면 총 EA 가 환산수량의 배수여야 정합하며(`TotalQuantity_Each % conversionQuantity == 0`),
 *    주문 박스 수량은 서버가 `TotalQuantity_Each / conversionQuantity` 로 역산한다.
 *
 * 모바일은 박스칸·낱개칸을 합산한 "총 EA"(레거시 `TotalQuantity_Each` 동등)만 단일 진실값으로 송신하고,
 * 박스 수량/단위 환산은 서버가 환산수량으로 재계산한다. 따라서 박스+낱개 혼합 입력도
 * 총 EA 가 환산수량의 배수이면 통과(박스로 흡수)하고, 떨어지지 않으면 차단한다(레거시 동등).
 */
object UnitConverter {

    private const val UNIT_EA = "EA"

    /**
     * 단위 환산 정합 검증 (레거시 OrderController.java:630-632).
     *
     * @return true 면 정합, false 면 위반(`ORD_INVALID_UNIT`)
     */
    fun isPiecesValid(unit: String, quantityPieces: Int, conversionQuantity: Int): Boolean {
        if (unit == UNIT_EA) return true
        val conv = conversionQuantity.coerceAtLeast(1)
        return quantityPieces % conv == 0
    }

    /**
     * 저장/SAP 송신용 박스 수량 (레거시 OrderController.java:641 — `TotalQuantity_Each / conversionQuantity`).
     *
     * EA 단위는 환산수량이 1 이므로 총 EA 가 그대로 반환된다. 그 외 단위는 [isPiecesValid] 로
     * 배수 검증을 통과한 뒤 호출하면 정수로 떨어진다(소수 2자리 컬럼 정합 위해 scale 2 보존).
     */
    fun toBoxQuantity(quantityPieces: Int, conversionQuantity: Int): BigDecimal {
        val conv = conversionQuantity.coerceAtLeast(1)
        return BigDecimal.valueOf(quantityPieces.toLong())
            .divide(BigDecimal.valueOf(conv.toLong()), 2, RoundingMode.HALF_UP)
    }
}
