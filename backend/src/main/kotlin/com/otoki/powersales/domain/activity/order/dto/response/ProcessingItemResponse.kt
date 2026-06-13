package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus

/**
 * 본인 주문요청 상세 — SAP 처리 라인 (Spec #595).
 *
 * 차량/기사 정보 5필드 (`driverName/vehicle/driverPhone/scheduleTime/completeTime`) 는 모바일 라인 탭 팝업용
 * (Q5 결정, 레거시 JSP `view.jsp:533-541` hidden input 동등). `WAITING` 라인은 모두 `null` 가능.
 *
 * 시각 필드(`scheduleTime`, `completeTime`) 는 `HH:mm` 형식 문자열이며, SAP `'000000'` sentinel 또는
 * 빈 값은 `null` 매핑.
 */
data class ProcessingItemResponse(
    val productCode: String,
    val productName: String,
    val deliveredQuantity: String,
    val deliveryStatus: DeliveryStatus,
    val driverName: String?,
    val vehicle: String?,
    val driverPhone: String?,
    val scheduleTime: String?,
    val completeTime: String?,
)
