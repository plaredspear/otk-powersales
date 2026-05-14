package com.otoki.powersales.order.enums

/**
 * 거래처 출하 주문 라인의 배송 상태.
 *
 * SAP 인바운드(#561)는 4개 SAP 필드(`DefaultReason`/`LineItemStatus`/`ShippingScheduleTime`/`ShippingCompleteTime`)
 * 조합으로 한글 4종을 파생해 `erp_order_product.delivery_status` 에 저장한다 (SapErpOrderService.computeDeliveryStatus).
 * 본 enum 은 응답 시점에 한글 → 영문 코드로 변환하기 위한 매핑이다.
 *
 * 한글 ↔ 영문 매핑 권위: SapErpOrderService.STATUS_* 상수.
 */
enum class DeliveryStatus(val koreanLabel: String) {
    PENDING("대기"),
    SHIPPING("배송중"),
    DELIVERED("배송 완료"),
    OUT_OF_STOCK("결품");

    companion object {
        /**
         * DB 에 저장된 한글 라벨을 enum 으로 변환한다. 미정의 라벨은 안전 기본값 [PENDING] 으로 fallback.
         */
        fun fromKoreanLabel(label: String?): DeliveryStatus {
            if (label.isNullOrBlank()) return PENDING
            return entries.firstOrNull { it.koreanLabel == label } ?: PENDING
        }
    }
}
