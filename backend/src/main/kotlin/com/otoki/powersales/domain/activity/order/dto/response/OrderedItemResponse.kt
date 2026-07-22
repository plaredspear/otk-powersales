package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — CRM 주문 라인 (`OrderRequestProduct` 매핑) (Spec #595, #845 P1-B).
 *
 * 취소요청(로컬) vs 실제(SAP) 비교를 라인 단위로 표시한다 (Spec #845):
 * - `isCancelRequested`(로컬 흔적, `cancel_requested_at != null`): 사용자가 취소를 요청한 라인.
 * - `isOutOfStock` / `outOfStockReason`: SAP `DefaultReason` 코드 ∈ {F1,L1,L2,L3} → "결품" 배지+사유.
 * - `isCancelledBySap` / `cancelReason`: SAP `DefaultReason` 코드 채워짐 && ∉ 결품셋 → "SAP취소됨" 배지+사유.
 * - `isCancelled`(`line_change_type='X'`): 마이그레이션된 과거 취소 이력 표시용 (신규 취소엔 미사용).
 *
 * 한 라인의 SAP `DefaultReason` 은 1개이므로 `isOutOfStock` 과 `isCancelledBySap` 은 상호배타적이다.
 * `isCancelRequested`(로컬)는 이와 독립적으로 함께 켜질 수 있다(비교의 핵심).
 */
data class OrderedItemResponse(
    val orderProductId: Long,
    val productCode: String?,
    val productName: String?,
    val totalQuantityBoxes: BigDecimal,
    val totalQuantityPieces: BigDecimal,
    val isCancelled: Boolean,
    val isCancelRequested: Boolean,
    val isOutOfStock: Boolean = false,
    val outOfStockReason: String? = null,
    val isCancelledBySap: Boolean = false,
    val cancelReason: String? = null,
) {
    companion object {
        /**
         * @param outOfStockReason 결품 사유 `"{코드} {설명}"` (결품셋 코드) — null 이면 결품 아님.
         * @param cancelReason 취소 사유 `"{코드} {설명}"` (취소 코드) — null 이면 SAP 취소 아님.
         */
        fun from(
            item: OrderRequestProduct,
            productName: String? = null,
            outOfStockReason: String? = null,
            cancelReason: String? = null,
        ): OrderedItemResponse =
            OrderedItemResponse(
                orderProductId = item.id,
                productCode = item.productCode,
                productName = productName ?: item.product?.name,
                // SF nillable=true 정합으로 수량이 nullable — 응답은 0 으로 보정 (기존 의미 보존).
                totalQuantityBoxes = item.quantityBoxes ?: BigDecimal.ZERO,
                totalQuantityPieces = item.quantityPieces ?: BigDecimal.ZERO,
                // 마이그레이션 과거 취소('X') 표시용 — 신규 취소는 로컬 확정하지 않으므로 세팅되지 않는다.
                isCancelled = item.isCancelled(),
                isCancelRequested = item.cancelRequestedAt != null,
                isOutOfStock = outOfStockReason != null,
                outOfStockReason = outOfStockReason,
                isCancelledBySap = cancelReason != null,
                cancelReason = cancelReason,
            )
    }
}
