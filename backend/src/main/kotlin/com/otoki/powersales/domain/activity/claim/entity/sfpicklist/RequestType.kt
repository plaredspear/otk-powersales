package com.otoki.powersales.domain.activity.claim.entity.sfpicklist

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF multipicklist `DKRetail__Claim__c.DKRetail__RequestType__c` enum (6 옵션).
 *
 * label == value (모두 한국어 원본).
 * multipicklist 이므로 entity 에는 `Set<RequestType>` 으로 보관, DB 에는 ";"-구분 CSV 로 저장.
 * SF 원본 옵션값을 보존 — README §6.6 v2.2 정책 준수.
 */
enum class RequestType(
    val displayName: String
) {
    OPINION_REPORT("의견서"),
    CONSULTATION("상담"),
    URGENT_FS("긴급처리(FS사업부)"),
    SALE_CANCEL_NEEDED("판매취소 필요"),
    PRODUCTION_SCHEDULE_ADJUSTMENT("생산일정 조율 필요"),
    COLLECTION_REQUEST("물량수거 요청");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): RequestType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 요청유형: $value")

        fun fromDisplayNameOrNull(value: String?): RequestType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
