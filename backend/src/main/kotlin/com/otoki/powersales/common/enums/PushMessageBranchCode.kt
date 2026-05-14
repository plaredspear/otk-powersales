package com.otoki.powersales.common.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `PushMessage__c.BranchCode__c` (지점코드) picklist enum.
 *
 * 단일 권위: Salesforce describe 메타 (`PushMessage__c`) picklistValues
 * Spec #709 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 * enum constant: 숫자 시작 불가 → CODE_{value} 접두사 적용.
 */
enum class PushMessageBranchCode(
    val displayName: String
) {
    CODE_4967("4967"),
    CODE_4971("4971"),
    CODE_4968("4968"),
    CODE_4969("4969"),
    CODE_4970("4970"),
    CODE_3236("3236"),
    CODE_3235("3235"),
    CODE_3234("3234"),
    CODE_3233("3233"),
    CODE_3231("3231"),
    CODE_3230("3230"),
    CODE_3229("3229"),
    CODE_3228("3228"),
    CODE_5460("5460"),
    CODE_5459("5459"),
    CODE_5458("5458"),
    CODE_5466("5466"),
    CODE_5467("5467"),
    CODE_5468("5468"),
    CODE_3844("3844"),
    CODE_5462("5462"),
    CODE_5463("5463"),
    CODE_5464("5464"),
    CODE_5465("5465"),
    CODE_2649("2649"),
    CODE_2650("2650"),
    CODE_5461("5461"),
    CODE_5455("5455"),
    CODE_5456("5456"),
    CODE_5457("5457"),
    CODE_5483("5483"),
    CODE_5482("5482"),
    CODE_5481("5481"),
    CODE_5480("5480"),
    CODE_5479("5479"),
    CODE_5475("5475"),
    CODE_5476("5476"),
    CODE_5477("5477"),
    CODE_5470("5470"),
    CODE_5471("5471"),
    CODE_5472("5472"),
    CODE_5473("5473"),
    CODE_5452("5452"),
    CODE_5453("5453"),
    CODE_5454("5454"),
    CODE_4148("4148"),
    CODE_3996("3996"),
    CODE_4027("4027"),
    CODE_5485("5485"),
    CODE_3997("3997"),
    CODE_5346("5346"),
    CODE_5349("5349"),
    CODE_5350("5350"),
    CODE_5348("5348"),
    CODE_5347("5347"),
    CODE_4609("4609"),
    CODE_4147("4147"),
    CODE_599("599"),
    CODE_2651("2651"),
    CODE_3989("3989"),
    CODE_5484("5484"),
    CODE_3987("3987"),
    CODE_3986("3986"),
    CODE_3457("3457"),
    CODE_1714("1714"),
    CODE_312("312"),
    CODE_313("313"),
    CODE_2615("2615"),
    CODE_309("309"),
    CODE_310("310"),
    CODE_311("311"),
    CODE_4889("4889");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): PushMessageBranchCode =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 BranchCode: $value")

        fun fromDisplayNameOrNull(value: String?): PushMessageBranchCode? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
