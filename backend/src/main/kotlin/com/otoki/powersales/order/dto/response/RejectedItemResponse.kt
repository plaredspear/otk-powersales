package com.otoki.powersales.order.dto.response

import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — 반려/결품 라인 (Spec #595).
 *
 * 레거시 derived `SAP_Status` 5분기 중 `반려` (평가 1) / `결품` (평가 5) 두 케이스를 별도 차원으로 분리.
 *
 * - `반려`: SAP 응답에서 `SAPOrderNumber` 빈 값 + `LineItemStatus` 채워진 라인. `rejectionReason = LineItemStatus`.
 * - `결품`: SAP 응답에서 `DefaultReason` 채워진 라인. `rejectionReason = DefaultReason`.
 */
data class RejectedItemResponse(
    val productCode: String,
    val productName: String,
    val orderQuantityBoxes: BigDecimal,
    val rejectionReason: String,
)
