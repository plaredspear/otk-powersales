package com.otoki.powersales.domain.foundation.product.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Product__c.DKRetail__ProductStatus__c` picklist enum.
 *
 * SF picklist **정의**에는 placeholder `-` 1개만 있으나(field-meta.xml / prod describe 양쪽),
 * 운영 데이터에는 `출고중지` 가 적재되어 있다 — SF UI 로 추가되었거나 마이그레이션 시 유입된 값.
 * 따라서 enum 은 **SF 정의가 아니라 운영 실측값**을 기준으로 상수를 갖는다.
 *
 * [displayName] 은 DB 저장값(SF 원본값), [label] 은 화면 표시명이다. 제품상태는 저장값과
 * 사용자 표기가 다르다 — 값이 없으면 "판매중", `출고중지` 는 "단종" 으로 보여준다.
 */
enum class ProductStatus(
    val displayName: String,
    val label: String
) {
    PLACEHOLDER("-", "판매중"),
    OUT_OF_STOCK("출고중지", "단종");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        /** 값이 없는(null) 제품의 화면 표시명 — 운영상 4187건이 여기 해당한다. */
        const val DEFAULT_LABEL: String = "판매중"

        /** 화면 표시명 전량 (중복 제거) — 필터 드롭다운 선택지. */
        fun labels(): List<String> = entries.map { it.label }.distinct()

        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProductStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 제품 상태: $value")

        fun fromDisplayNameOrNull(value: String?): ProductStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }

        /** 화면 표시명 → enum. 필터 파라미터가 표시명으로 오므로 역방향 해소가 필요하다. */
        fun fromLabelOrNull(value: String?): ProductStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.label == value }
        }
    }
}
