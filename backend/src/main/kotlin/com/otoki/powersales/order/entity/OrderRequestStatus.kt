package com.otoki.powersales.order.entity

/**
 * 주문요청 상태 enum
 *
 * 라이프사이클: DRAFT → SENT → APPROVED 또는 SEND_FAILED → (사용자 취소 시) CANCELED
 *
 * SF Picklist `DKRetail__RequestStatus__c` 매핑:
 *  - APPROVED    : 레거시 '승인완료' (IF_Util.cls:193, 202)
 *  - SEND_FAILED : 레거시 '전송실패' (IF_Util.cls:196, 205)
 *  - DRAFT/SENT/CANCELED 는 신규 시스템 자체 상태 (SF 측 매핑값은 운영 데이터로 추후 확정).
 */
enum class OrderRequestStatus {
    DRAFT,
    SENT,
    APPROVED,
    SEND_FAILED,
    CANCELED,
}
