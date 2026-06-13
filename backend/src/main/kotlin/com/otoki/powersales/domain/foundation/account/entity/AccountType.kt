package com.otoki.powersales.domain.foundation.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.Type` (거래처유형) enum.
 *
 * 단일 권위: 운영 `Account.Type` 실제 저장값 = `AccountCategoryMaster.Name` (거래처유형마스터의 거래처구분, 18종).
 *
 * `displayName` 은 SF picklist *정의 라벨* 이 아니라 **운영 실제 저장 raw 값**(거래처유형마스터 Name 과 동일 문자열)이어야 한다.
 * SF `SalesComparisonSearchController` 등은 `Account.Type` raw 값을 `AccountCategoryMaster.Name` 과 직접 매칭하므로
 * (`categoryMap.get(Account__r.Type)`), displayName 이 마스터 Name 과 다르면 거래처유형 매칭이 전량 실패(null)하여
 * 배치적합성/대시보드 거래처유형 집계가 SF 와 어긋난다.
 *
 *   - enum name 은 영문 (컨벤션 보존)
 *   - DB 저장값 + JSON 직렬화는 운영 실제값 (`displayName`, 거래처유형마스터 Name 정합)
 *   - JPA 매핑은 `AccountTypeConverter` 경유
 *   - account_code 는 거래처유형마스터 AccountCode__c 와 1:1 (화면 카테고리 컬럼 매핑 source)
 */
enum class AccountType(
    val displayName: String,
    val accountCode: String
) {
    DISCOUNT_STORE("대형마트(3대)", "01"),
    CHAIN("체인", "02"),
    DEPARTMENT_STORE("백화점", "03"),
    CVS("C.V.S", "04"),
    NONGHYUP("농협", "05"),
    SUPER("슈퍼", "06"),
    AGENCY("대리점", "07"),
    WHOLESALE("홀세일", "08"),
    CONVENIENCE_STORE("편의점", "09"),
    FOOD_MATERIAL("식자재", "10"),
    GROUP_FEEDING("단체급식", "11"),
    OIL_CONFECTIONERY("유지베이커리", "12"),
    RESTAURANT("외식", "13"),
    MANUFACTURING("제조", "14"),
    MILITARY("군납", "15"),
    OTHER("기타", "16"),
    ONLINE("온라인", "19"),
    EXPORT("수출", "20");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): AccountType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 거래처유형: $value")

        fun fromDisplayNameOrNull(value: String?): AccountType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
