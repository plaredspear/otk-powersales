package com.otoki.powersales.common.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `PushMessage__c.Branch__c` (지점/팀) picklist enum.
 *
 * 단일 권위: Salesforce describe 메타 (`PushMessage__c`) picklistValues
 * Spec #709 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class PushMessageBranch(
    val displayName: String
) {
    E_BIZ_TEAM1("[E-Biz영업부] E-Biz영업부-1팀"),
    E_BIZ_SUPPORT_TEAM("[E-Biz영업부] E-Biz영업부-지원팀"),
    E_BIZ_TEAM2("[E-Biz영업부] E-Biz영업부-2팀"),
    E_BIZ_TEAM3("[E-Biz영업부] E-Biz영업부-3팀"),
    E_BIZ_PRODUCT_DEV_TEAM("[E-Biz영업부] E-Biz영업부-제품개발팀"),
    DISTRIBUTION_DEPT2_TEAM4("[유통총괄실] 유통총괄2부-4팀"),
    DISTRIBUTION_DEPT2_TEAM3("[유통총괄실] 유통총괄2부-3팀"),
    DISTRIBUTION_DEPT2_TEAM2("[유통총괄실] 유통총괄2부-2팀"),
    DISTRIBUTION_DEPT2_TEAM1("[유통총괄실] 유통총괄2부-1팀"),
    DISTRIBUTION_DEPT1_TEAM4("[유통총괄실] 유통총괄1부-4팀"),
    DISTRIBUTION_DEPT1_TEAM3("[유통총괄실] 유통총괄1부-3팀"),
    DISTRIBUTION_DEPT1_TEAM2("[유통총괄실] 유통총괄1부-2팀"),
    DISTRIBUTION_DEPT1_TEAM1("[유통총괄실] 유통총괄1부-1팀"),
    DIV1_DEPT3_GANGNAM_DIST_BRANCH("[제1사업부] 3영업부-강남유통지점"),
    DIV1_DEPT3_GANGNAM_BRANCH2("[제1사업부] 3영업부-강남2지점"),
    DIV1_DEPT3_GANGNAM_BRANCH1("[제1사업부] 3영업부-강남1지점"),
    DIV1_DEPT6_DAEJEON_BRANCH1("[제1사업부] 6영업부-대전1지점"),
    DIV1_DEPT6_CHEONAN_BRANCH1("[제1사업부] 6영업부-천안1지점"),
    DIV1_DEPT6_DAEJEON_DIST_BRANCH("[제1사업부] 6영업부-대전유통지점"),
    DIV1_DEPT5_WONJU_BRANCH1("[제1사업부] 5영업부-원주1지점"),
    DIV1_DEPT5_GYEONGGI_BRANCH1("[제1사업부] 5영업부-경기1지점"),
    DIV1_DEPT5_GYEONGGI_BRANCH2("[제1사업부] 5영업부-경기2지점"),
    DIV1_DEPT5_GYEONGGI_DIST_BRANCH("[제1사업부] 5영업부-경기유통지점"),
    DIV1_DEPT5_WONJU_DIST_BRANCH("[제1사업부] 5영업부-원주유통지점"),
    DIV1_DEPT4_INCHEON_BRANCH1("[제1사업부] 4영업부-인천1지점"),
    DIV1_DEPT4_INCHEON_BRANCH2("[제1사업부] 4영업부-인천2지점"),
    DIV1_DEPT4_INCHEON_DIST_BRANCH("[제1사업부] 4영업부-인천유통지점"),
    DIV1_DEPT2_GANGBUK_BRANCH1("[제1사업부] 2영업부-강북1지점"),
    DIV1_DEPT2_GANGBUK_BRANCH2("[제1사업부] 2영업부-강북2지점"),
    DIV1_DEPT2_GANGBUK_DIST_BRANCH("[제1사업부] 2영업부-강북유통지점"),
    DIV1_DEPT9_CHANGWON_DIST_BRANCH("[제1사업부] 9영업부-창원유통지점"),
    DIV1_DEPT9_BUSAN_DIST_BRANCH("[제1사업부] 9영업부-부산유통지점"),
    DIV1_DEPT9_CHANGWON_BRANCH1("[제1사업부] 9영업부-창원1지점"),
    DIV1_DEPT9_ULSAN_BRANCH1("[제1사업부] 9영업부-울산1지점"),
    DIV1_DEPT9_BUSAN_BRANCH1("[제1사업부] 9영업부-부산1지점"),
    DIV1_DEPT8_DAEGU_BRANCH1("[제1사업부] 8영업부-대구1지점"),
    DIV1_DEPT8_DAEGU_BRANCH2("[제1사업부] 8영업부-대구2지점"),
    DIV1_DEPT8_DAEGU_DIST_BRANCH("[제1사업부] 8영업부-대구유통지점"),
    DIV1_DEPT7_GWANGJU_BRANCH1("[제1사업부] 7영업부-광주1지점"),
    DIV1_DEPT7_JEONJU_BRANCH1("[제1사업부] 7영업부-전주1지점"),
    DIV1_DEPT7_JEJU_BRANCH("[제1사업부] 7영업부-제주지점"),
    DIV1_DEPT7_GWANGJU_DIST_BRANCH("[제1사업부] 7영업부-광주유통지점"),
    DIV1_DEPT1_GANGSEO_BRANCH1("[제1사업부] 1영업부-강서1지점"),
    DIV1_DEPT1_GANGSEO_BRANCH2("[제1사업부] 1영업부-강서2지점"),
    DIV1_DEPT1_GANGSEO_DIST_BRANCH("[제1사업부] 1영업부-강서유통지점"),
    DIV4_DEPT6_JINJU_TEAM3("[제4사업부] 6영업부-진주3팀"),
    DIV4_DEPT6_DAEGU_BRANCH3("[제4사업부] 6영업부-대구3지점"),
    DIV4_DEPT6_DAEGU_BRANCH4("[제4사업부] 6영업부-대구4지점"),
    DIV4_DEPT6_BUSAN_BRANCH2("[제4사업부] 6영업부-부산2지점"),
    DIV4_DEPT6_CHANGWON_BRANCH2("[제4사업부] 6영업부-창원2지점"),
    DIV4_DEPT6_ULSAN_BRANCH2("[제4사업부] 6영업부-울산2지점"),
    DIV4_DEPT7_JEONJU_BRANCH2("[제4사업부] 7영업부-전주2지점"),
    DIV4_DEPT7_GWANGJU_BRANCH2("[제4사업부] 7영업부-광주2지점"),
    DIV4_DEPT7_SUNCHEON_TEAM3("[제4사업부] 7영업부-순천3팀"),
    DIV4_DEPT7_JEJU_TEAM3("[제4사업부] 7영업부-제주3팀"),
    DIV4_DEPT5_CHEONAN_BRANCH2("[제4사업부] 5영업부-천안2지점"),
    DIV4_DEPT5_CHEONGJU_BRANCH2("[제4사업부] 5영업부-청주2지점"),
    DIV4_DEPT5_DAEJEON_BRANCH2("[제4사업부] 5영업부-대전2지점"),
    DIV4_DEPT4_INCHEON_BRANCH3("[제4사업부] 4영업부-인천3지점"),
    DIV4_DEPT4_WONJU_BRANCH2("[제4사업부] 4영업부-원주2지점"),
    DIV4_DEPT4_GYEONGGI_BRANCH3("[제4사업부] 4영업부-경기3지점"),
    DIV4_DEPT3_SALES_BRANCH7("[제4사업부] 3영업부-3영업7지점"),
    DIV4_DEPT3_SALES_BRANCH5("[제4사업부] 3영업부-3영업5지점"),
    DIV4_DEPT3_SALES_BRANCH6("[제4사업부] 3영업부-3영업6지점"),
    DIV4_DEPT2_SECTION3("[제4사업부] 2영업부-2영업3과"),
    DIV4_DEPT2_SECTION1("[제4사업부] 2영업부-2영업1과"),
    DIV4_DEPT2_SECTION2("[제4사업부] 2영업부-2영업2과"),
    DIV4_DEPT2_SECTION4("[제4사업부] 2영업부-2영업4과"),
    DIV4_DEPT1_SECTION1("[제4사업부] 1영업부-1영업1과"),
    DIV4_DEPT1_SECTION2("[제4사업부] 1영업부-1영업2과"),
    DIV4_DEPT1_SECTION3("[제4사업부] 1영업부-1영업3과"),
    SALES_HQ_SUPPORT_TEAM2("[영업본부] 영업지원실-영업지원2팀");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): PushMessageBranch =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 Branch: $value")

        fun fromDisplayNameOrNull(value: String?): PushMessageBranch? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
