package com.otoki.internal.entity

/**
 * 배송 상태 Enum (SAP 주문 처리 현황)
 */
enum class DeliveryStatus {
    WAITING,    // 대기
    SHIPPING,   // 배송중
    DELIVERED   // 배송완료
}
