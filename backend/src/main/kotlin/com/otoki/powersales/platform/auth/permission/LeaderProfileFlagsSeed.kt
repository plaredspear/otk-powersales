package com.otoki.powersales.platform.auth.permission

/**
 * 조장 계열 Profile 의 ProfileFlags 초기값 SoT (declarative reference data).
 *
 * "조장 프로파일이 어떤 권한을 가져야 하는가" 의 단일 진실은 SF 원본 재적재가 아니라 본 코드 상수다.
 * dev 에서 확정한 조장 권한 상태를 그대로 고정하여 모든 환경(dev / prod)에 재현한다 —
 * CLAUDE.md §4 reference data 정책 정합 (Flyway INSERT 금지 / Kotlin SoT + 부팅 sync 패턴).
 * `SavedSearchPresets` / `SystemAdminProfilePolicy` 와 동일 접근.
 *
 * ## 왜 SF 재적재가 아니라 SoT 인가
 * ProfileFlags 의 system 비트 / object_permissions 는 SF Profile XML + ObjectPermissions SOQL 을 출처로
 * Stage1/Stage2 로 적재되지만, 신규 시스템에서 조장에 부여하려는 권한은 SF frozen snapshot 을 고쳐
 * 재적재할 수 없다 (기존소스 불변). 또한 ProfileFlags 는 web admin 편집 시 `is_locally_modified=TRUE` 로
 * SF 재적재로부터 보호되는 구조라, 신규 권한은 SF 출처가 아닌 신규 SoT 로 관리하는 것이 정합이다.
 *
 * ## 적용 규칙 (초기값-only)
 * [LeaderProfileFlagsSyncRunner] 가 부팅 시 본 SoT 를 순회하되, 대상 Profile 의 ProfileFlags row 가
 * **없거나 `is_locally_modified=FALSE` 일 때만** 적용한다. web admin 으로 편집한 dirty row 는
 * 건드리지 않아 운영 편집 자율성을 보존한다 (SF 재적재 dirty-skip 정책과 동일).
 *
 * ## JSON 형식 보존
 * [objectPermissionsJson] / [customPermissionsJson] 은 dev 실측값을 그대로 보존한다.
 * `6.조장` 은 present-key sparse 형식 (`{"allowRead": true}`), `7.영업사원 + 조장` 은 6비트 full
 * 형식으로 서로 다르나, 둘 다 dev 확정 상태이므로 원문 그대로 저장한다 (SfPermissionResolver 가
 * 두 형식을 모두 해석).
 */
object LeaderProfileFlagsSeed {

    /**
     * profile.name (자연 키) → 초기 ProfileFlags 값.
     *
     * system 비트 5종 중 dev 실측상 `api_enabled` 만 true (나머지 false). object/custom 권한은
     * dev 실측 JSON 원문.
     */
    val SEEDS: List<Seed> = listOf(
        Seed(
            profileName = "6.조장",
            viewAllData = false,
            modifyAllData = false,
            viewAllUsers = false,
            manageUsers = false,
            apiEnabled = true,
            objectPermissionsJson = LEADER_6_OBJECT_PERMISSIONS,
            customPermissionsJson = LEADER_6_CUSTOM_PERMISSIONS,
        ),
        Seed(
            profileName = "7.영업사원 + 조장",
            viewAllData = false,
            modifyAllData = false,
            viewAllUsers = false,
            manageUsers = false,
            apiEnabled = true,
            objectPermissionsJson = LEADER_7_OBJECT_PERMISSIONS,
            customPermissionsJson = null,
        ),
    )

    data class Seed(
        val profileName: String,
        val viewAllData: Boolean,
        val modifyAllData: Boolean,
        val viewAllUsers: Boolean,
        val manageUsers: Boolean,
        val apiEnabled: Boolean,
        val objectPermissionsJson: String,
        val customPermissionsJson: String?,
    )
}

// dev 실측 조장(6.조장) object_permissions — present-key sparse 형식 (미기재 비트 = false).
private val LEADER_6_OBJECT_PERMISSIONS = """
{
  "Org__c": { "allowRead": true },
  "Account": { "allowRead": true },
  "Theme__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "ERP_Order__c": { "allowRead": true },
  "AttendInfo__c": { "allowRead": true },
  "UploadFile__c": { "allowRead": true },
  "PushMessage__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "StaffReview__c": { "allowEdit": true, "allowRead": true, "allowCreate": true },
  "ProductBarcode__c": { "allowRead": true },
  "DKRetail__Claim__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "DKRetail__Notice__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "ERP_OrderProduct__c": { "allowRead": true },
  "DKRetail__Product__c": { "allowRead": true },
  "DailySalesHistory__c": { "allowRead": true },
  "DKRetail__Employee__c": { "allowEdit": true, "allowRead": false },
  "DKRetail__Proposal__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "DKRetail__Promotion__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "MonthlySalesHistory__c": { "allowRead": true },
  "PushMessageReceiver__c": { "allowRead": true },
  "DKRetail__CommuteLog__c": { "allowRead": true },
  "AccountCategoryMaster__c": { "allowRead": true },
  "DKRetail__OrderRequest__c": { "allowRead": true },
  "DKRetail__SiteAcitivity__c": { "allowRead": true },
  "SalesProgressRateMaster__c": { "allowRead": true },
  "DisplayWorkScheduleMaster__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "DKRetail__PromotionProduct__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "DKRetail__PromotionEmployee__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true },
  "EmployeeInputCriteriaMaster__c": { "allowRead": true },
  "DKRetail__TeamMemberSchedule__c": { "allowRead": true, "allowCreate": true },
  "DKRetail__OrderRequestProduct__c": { "allowRead": true },
  "ProfessionalPromotionTeamMaster__c": { "allowRead": true },
  "ProfessionalPromotionTeamHistory__c": { "allowRead": true },
  "MonthlyFemaleEmployeeIntegrationSchedule__c": { "allowEdit": true, "allowRead": true, "allowCreate": true }
}
""".trimIndent()

// dev 실측 조장(6.조장) custom_permissions — 가상자원(@PermissionResource) 권한.
private val LEADER_6_CUSTOM_PERMISSIONS = """
{
  "female_employee": { "allowEdit": true, "allowRead": true, "allowCreate": true }
}
""".trimIndent()

// dev 실측 영업사원+조장(7.영업사원 + 조장) object_permissions — 6비트 full 형식.
private val LEADER_7_OBJECT_PERMISSIONS = """
{
  "Org__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "Survey": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "Account": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "Contact": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "Theme__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": true, "modifyAllRecords": false },
  "HQReview__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "ERP_Order__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "UploadFile__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "PushMessage__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "StaffReview__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "SurveyResponse": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "BranchReview__c": { "allowEdit": true, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "SurveyInvitation": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "ProductBarcode__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "DKRetail__Claim__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": true, "modifyAllRecords": false },
  "DKRetail__Notice__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "ERP_OrderProduct__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "DKRetail__Product__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "DailySalesHistory__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "TestReportRequest__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__Employee__c": { "allowEdit": true, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__Proposal__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__Promotion__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": true, "modifyAllRecords": true },
  "MonthlySalesHistory__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": true, "modifyAllRecords": false },
  "PushMessageReceiver__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__CommuteLog__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__SalesDiary__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__SiteAcitivity__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "DisplayWorkScheduleMaster__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__PromotionEmployee__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": true, "modifyAllRecords": true },
  "EmployeeInputCriteriaMaster__c": { "allowEdit": false, "allowRead": true, "allowCreate": false, "allowDelete": false, "viewAllRecords": false, "modifyAllRecords": false },
  "DKRetail__TeamMemberSchedule__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false },
  "MonthlyFemaleEmployeeIntegrationSchedule__c": { "allowEdit": true, "allowRead": true, "allowCreate": true, "allowDelete": true, "viewAllRecords": false, "modifyAllRecords": false }
}
""".trimIndent()
