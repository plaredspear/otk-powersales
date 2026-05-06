package com.otoki.powersales.order.dto.response

/**
 * 본인 주문요청 상세 — SAP 주문번호별 처리 그룹 (Spec #595).
 *
 * 레거시 Heroku JSP `Map<SAP_SAPOrderNumber, List<라인>>` 그룹 섹션 1개에 1:1 매핑.
 * 다중 SAP 주문 분할 시 N개의 그룹이 응답 배열에 포함된다.
 */
data class OrderProcessingStatusResponse(
    val sapOrderNumber: String,
    val items: List<ProcessingItemResponse>,
)
