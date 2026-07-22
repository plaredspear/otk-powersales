package com.otoki.powersales.domain.activity.order.dto.response

import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — 결품 라인 (Spec #595 / 결품 전용 섹션 분리 2026-07-23).
 *
 * SAP 응답에서 `DefaultReason` 코드가 결품셋({F1,L1,L2,L3})으로 분류된 라인. 기존에는 "주문한 제품"
 * 목록에 회색 배지로 인라인 표시했으나, 사용자 결정으로 **반려 섹션처럼 별도 "결품 제품" 영역**으로
 * 분리하고 "주문한 제품" 목록에서는 제외한다.
 *
 * - `reason`: `"{코드} {설명}"` (예: `"L1 [물류] 재고부족"`) — `DefaultReasonCode.displayReason` 산출값.
 */
data class OutOfStockItemResponse(
    val productCode: String,
    val productName: String,
    val orderQuantityBoxes: BigDecimal,
    val reason: String,
)
