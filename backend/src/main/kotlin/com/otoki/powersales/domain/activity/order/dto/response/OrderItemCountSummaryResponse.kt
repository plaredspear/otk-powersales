package com.otoki.powersales.domain.activity.order.dto.response

/**
 * 주문 품목 수 집계 요약 (모바일 주문 상세 헤더 "승인된 품목 수" + info 팝업용).
 *
 * 집계 기준은 **CRM 주문 라인**([com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct])
 * 1건 = 1개다. 라인의 `productCode` 를 SAP 상세 응답 분류 결과에 대조해 아래 4분류 중 하나로 배정하며,
 * 어느 분류에도 안 걸린 라인(=SAP 응답에 아직 안 나온 라인)은 어떤 카운트에도 들어가지 않는다
 * — 팝업 각주 "납품문서가 생성된 품목만 집계됩니다" 가 이 잔여분을 설명한다.
 *
 * 분류 우선순위 (한 제품코드가 복수 분류에 걸릴 때): 반려 > 미납 > 취소 > 출고 확정.
 *
 * - [confirmedCount] 출고 확정 — SAP 응답에 SAPOrderNumber 가 채워졌고 `DefaultReason` 이 없는(=취소/미납
 *   아님) 정상 라인. 헤더 "승인된 품목 수" 값.
 * - [cancelledCount] 취소 — SAP `DefaultReason` 취소셋({L4,O1,S1,S2,S3}) 또는 마이그레이션 로컬 취소(`line_change_type='X'`).
 * - [outOfStockCount] 미납 — SAP `DefaultReason` 결품셋({F1,L1,L2,L3}).
 * - [rejectedCount] 반려 — SAPOrderNumber 빈 값 + LineItemStatus 채워진 라인.
 *
 * SAP 호출 실패/빈 응답이면 [orderedCount] 만 채워지고 나머지는 0 이다 (모바일은 "승인된 품목 수 0개" 로 노출).
 */
data class OrderItemCountSummaryResponse(
    /** 주문 라인 총수 — 반려/미납/취소 포함 전량. 팝업 "주문 N개 중" 의 N. */
    val orderedCount: Int,
    /** 출고 확정 품목 수 — 헤더 "승인된 품목 수" 값. */
    val confirmedCount: Int,
    val cancelledCount: Int,
    val outOfStockCount: Int,
    val rejectedCount: Int,
)
