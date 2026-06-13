package com.otoki.powersales.platform.auth.sharing

/**
 * SF SharingModel / ControlledByParent 정합 상수 (spec #782 P3-B).
 *
 * **DEPRECATED** (spec #791) — 본 const map 은 DB-driven 으로 전환. 신규 코드는
 * `SObjectSettingProvider` 사용. 본 파일은 backward compat 목적 잠시 유지 — DB 미적재 환경
 * (테스트 등) 의 fallback 또는 신규 OWD/parent 메타 추가 시 초기 seed 참고 자료.
 *
 * 출처: `_raw/<SObject>.json` describe API 의 `controllableByParent` / 메타 `sharingModel`.
 *
 * 본 프로젝트 운영 — 6 SObject 만 sharingRule 본문 보유. 나머지 233 SObject 는 빈 본문 (UserRole
 * Hierarchy 자동 적용만 사용).
 */
@Deprecated(
    "Use SObjectSettingProvider (DB-driven, spec #791). Const map is kept only for legacy reference.",
    ReplaceWith("SObjectSettingProvider"),
)
object SfSharingConstants {

    /**
     * SObject 별 sharingModel.
     * - Private — UserRole Hierarchy + Sharing Rule 자동 적용 대상.
     * - Read — internal user 전체 read 허용 (sharingRule 무관).
     * - ControlledByParent — 부모 SObject 의 sharingModel 에 따름. [SF_PARENT] 매핑 필수.
     */
    val SF_SHARING_MODEL: Map<String, String> = mapOf(
        "Account" to "Private",
        "BranchReview__c" to "Private",
        "DisplayWorkScheduleMaster__c" to "Private",
        "HQReview__c" to "Private",
        "MonthlyFemaleEmployeeIntegrationSchedule__c" to "Private",
        "SalesProgressRateMaster__c" to "Private",
        "DKRetail__Promotion__c" to "Private",
        "DKRetail__PromotionEmployee__c" to "ControlledByParent",
    )

    /**
     * ControlledByParent SObject 의 부모 매핑.
     * 자식 record 의 가시성 = 부모 record 의 가시성.
     */
    val SF_PARENT: Map<String, String> = mapOf(
        "DKRetail__PromotionEmployee__c" to "DKRetail__Promotion__c",
    )

    fun isControlledByParent(sObjectName: String): Boolean =
        SF_SHARING_MODEL[sObjectName] == "ControlledByParent"

    fun parentOf(sObjectName: String): String? = SF_PARENT[sObjectName]
}
