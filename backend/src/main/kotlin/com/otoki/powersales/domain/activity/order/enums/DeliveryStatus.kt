package com.otoki.powersales.domain.activity.order.enums

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
    // koreanLabel 은 **거래처주문 도메인**(SF inbound ClientOrderReceive.cls:158)의 표기 '배송 완료'(공백)
    // 에 묶여 있다 — erp_order_product.delivery_status 저장값이자 fromKoreanLabel 매칭 키라 바꾸면
    // 거래처주문 조회가 깨진다. 주문상세(SF 조회 클래스 cls:157 은 공백 없는 '배송완료')의 화면 표기는
    // 별도로 클라이언트(주문상세 처리현황 위젯)에서 매핑한다 — 두 도메인 표기가 SF 원문상 다르기 때문.
    DELIVERED("배송 완료"),
    OUT_OF_STOCK("결품"),

    // 취소 — SAP DefaultReason 코드가 취소셋({L4,O1,S1,S2,S3})으로 분류된 라인 (2026-07-23 사용자 결정).
    // 결품셋({F1,L1,L2,L3})은 OUT_OF_STOCK, 그 외 DefaultReason 은 이 CANCELLED 로 표기한다.
    //  - 내 주문 상세: OrderRequestDetailMapper 가 코드로 직접 지정.
    //  - 거래처주문: ErpOrderUpsertService 가 delivery_status='취소' 로 저장 → fromKoreanLabel("취소")=CANCELLED.
    CANCELLED("취소"),

    // 레거시 조회 클래스 cls:153-159 는 5개 독립 if 로 상태를 파생하며, 어느 조건에도 안 걸리면
    // status 가 최종 ''(빈 문자열)로 남는다 (예: 정상 라인이지만 LineItemStatus 만 채워지고 배차/완료
    // 시각이 없는 케이스). 신규는 이를 '대기'로 뭉개지 않고 UNKNOWN(빈 라벨)으로 구분해 SF 정합을 맞춘다.
    UNKNOWN("");

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
