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

    /**
     * 주문 상세 **표시용** 박스 수량 (레거시 `CRM_TotalQuantity_Box` 정합).
     *
     * 레거시 확정: `CRM_TotalQuantity_Box = 총EA(TotalQuantity_Each) ÷ 박스입수(Product.BoxReceivingQuantity)`,
     * `write.jsp:215-216` `qBox.toFixed(2)` → 소수 2자리 반올림. 분모는 **제품 마스터 박스입수**이며
     * SAP InventorySearch `ConversionQuantity`(환산수량)가 **아니다**. 두 값은 SF 에서 별개 필드로,
     * "10구" 계란류처럼 박스입수 ≠ 환산수량인 제품에서 갈린다.
     *
     * 저장된 [storedBoxes](`quantity_boxes` = 총EA÷환산수량, SAP 송신용)를 표시에 쓰면 위 제품에서
     * 어긋나므로(예: 10÷10=1 대신 10÷20=0.5), 표시값은 박스입수로 재파생한다. SAP 송신 경로
     * (`OrderRequestRegisterSender` = 총EA÷환산수량)는 그대로 두어 레거시 `orderQuantity` 와 정합 유지.
     *
     * @param boxReceivingQuantity 제품 마스터 박스입수(박스당 낱개수). null/0 이면 재파생 불가로 [storedBoxes] 폴백.
     */
    fun toDisplayBoxQuantity(
        quantityPieces: BigDecimal?,
        boxReceivingQuantity: BigDecimal?,
        storedBoxes: BigDecimal?,
    ): BigDecimal {
        val fallback = storedBoxes ?: BigDecimal.ZERO
        val pieces = quantityPieces ?: return fallback
        if (boxReceivingQuantity == null || boxReceivingQuantity <= BigDecimal.ZERO) return fallback
        return pieces.divide(boxReceivingQuantity, 2, RoundingMode.HALF_UP)
    }
}
