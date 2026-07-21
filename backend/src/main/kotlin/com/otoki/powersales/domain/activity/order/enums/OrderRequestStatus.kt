package com.otoki.powersales.domain.activity.order.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 주문요청 상태 enum
 *
 * 라이프사이클: DRAFT → SENT → APPROVED 또는 SEND_FAILED → (사용자 취소 시) CANCELED
 *
 * SF Picklist `DKRetail__RequestStatus__c` 매핑 (Spec #753 STEP4):
 *  - DRAFT       ↔ 임시저장
 *  - SENT        ↔ 전송
 *  - APPROVED    ↔ 승인완료 (레거시 IF_Util.cls:193, 202)
 *  - SEND_FAILED ↔ 전송실패 (레거시 IF_Util.cls:196, 205)
 *  - CANCELED    ↔ 주문취소
 *
 * DB 저장값은 SF 한국어 원본 (displayName) — README §6.6 v2.2 정책 준수.
 */
enum class OrderRequestStatus(
    val displayName: String
) {
    DRAFT("임시저장"),
    SENT("전송"),
    APPROVED("승인완료"),
    SEND_FAILED("전송실패"),
    CANCELED("주문취소");

    @JsonValue
    fun toJson(): String = displayName

    /**
     * API 응답 표시명 (모바일 노출용).
     *
     * 원본 표시명이 영업사원에게 오해를 줄 수 있는 상태만 **응답 표시명을** 재정의한다:
     *  - `APPROVED`("승인완료") → "전송완료" (실제 승인은 SF/SAP 측 자동 처리)
     *  - `CANCELED`("주문취소") → "주문취소요청완료" (실제 취소는 SAP 접수 후 시간이 걸릴 수 있음)
     *
     * DB 저장값(`OrderRequestStatusConverter`)·SF Picklist 매핑(`fromDisplayName`)·[toJson] 직렬화는
     * [displayName](원본, 예: "승인완료"/"주문취소")을 그대로 사용한다.
     */
    val clientDisplayName: String
        get() = when (this) {
            APPROVED -> "전송완료"
            CANCELED -> "주문취소요청완료"
            else -> displayName
        }

    companion object {
        /** 취소 가능 상태 (Spec #597). `DRAFT` / `CANCELED` 는 취소 불가. */
        val CANCELLABLE: Set<OrderRequestStatus> = setOf(SENT, APPROVED, SEND_FAILED)

        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): OrderRequestStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 주문요청 상태: $value")

        fun fromDisplayNameOrNull(value: String?): OrderRequestStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
