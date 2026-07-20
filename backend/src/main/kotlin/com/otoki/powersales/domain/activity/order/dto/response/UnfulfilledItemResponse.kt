package com.otoki.powersales.domain.activity.order.dto.response

import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — 미납 제품 라인 (신규 정책, 2026-07-20 사용자 결정 — SF 레거시엔 없던 섹션).
 *
 * SAP `OrderRequestDetail` 응답에서 `SAPOrderNumber` 가 **있는** 라인 중 `LineItemStatus` 가
 * 채워져 있으면서 `"OK"` 가 아닌 라인. 판정 축 구분:
 * - 반려(`SAPOrderNumber` 빈 값 && `LineItemStatus` 채워짐)와는 SAPOrderNumber 유무로 겹치지 않는다.
 * - 결품(`DefaultReason` 존재)은 제외 — 결품은 기존 표시(주문한 제품 회색 + 처리현황 '미납' 상태) 유지.
 * - `LineItemStatus` 빈 값(정상 대기 라인)은 미납이 아니다.
 *
 * 미납 라인은 처리현황 그룹에도 그대로 남는다(이중 표시 — 그룹 완전성 유지).
 *
 * @property reason SAP `LineItemStatus` 원문 (무가공)
 */
data class UnfulfilledItemResponse(
    val productCode: String,
    val productName: String,
    val orderQuantityBoxes: BigDecimal,
    val reason: String,
)
