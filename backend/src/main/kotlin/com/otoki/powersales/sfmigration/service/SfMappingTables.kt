package com.otoki.powersales.sfmigration.service

/**
 * SF 데이터 마이그레이션 매핑 표 (한글 picklist → enum). 1회성 cut-over 용도라
 * sfmigration 도메인 내부에 상수로 보관 — 폐기 시 도메인 통째 삭제로 일관 청소.
 *
 * 매핑 source: scripts/sf-data-migration/common.kts (런칭 후 폐기).
 * SoT 정합 검증: UserRole / ProfessionalPromotionTeamType / ProfileType enum 의 한글 라벨과 일치.
 */

internal val APP_AUTHORITY_TO_USER_ROLE: Map<String, String> = mapOf(
    "여사원" to "WOMAN",
    "조장" to "LEADER",
    "지점장" to "BRANCH_MANAGER",
    "영업부장" to "SALES_MANAGER",
    "사업부장" to "BUSINESS_MANAGER",
    "영업본부장" to "HEADQUARTERS_MANAGER",
    "영업지원실" to "SALES_SUPPORT",
    "시스템관리자" to "SYSTEM_ADMIN",
    "AccountViewAll" to "ACCOUNT_VIEW_ALL",
)
internal const val USER_ROLE_FALLBACK = "UNKNOWN"

internal val PERMISSION_SET_TO_PERMISSIONS: Map<String, List<String>> = mapOf(
    "Employee_View_All" to listOf("EMPLOYEE_READ"),
    "Activity_View_All" to listOf("SCHEDULE_READ"),
    "SalesProgressViewAll" to listOf("ACCOUNT_READ"),
    "View_All" to listOf("AGREEMENT_READ"),
    "View_All_Edit_All" to listOf("SAFETY_CHECK_READ"),
    "View_ALL_EVENT" to listOf("SCHEDULE_READ"),
    "View_All_TeamMemberSchedule" to listOf("SCHEDULE_READ"),
    "Acc_Permission" to listOf("ACCOUNT_READ"),
    "Object_View_All" to listOf("ACCOUNT_READ", "EMPLOYEE_READ"),
    "SalesDiary_View_All" to listOf("SCHEDULE_READ"),
    "Promotion_Master_View_All" to listOf("PROMOTION_READ"),
    "Claim_View_All" to listOf("ACCOUNT_READ"),
    "notification_View_All" to listOf("DASHBOARD_READ"),
    "CVSCLAIMDELETE" to listOf("ACCOUNT_DELETE"),
    "Uploadfile_Create_Delete_Permission" to listOf("PROMOTION_WRITE"),
    "SalesAssistant" to listOf("DASHBOARD_READ", "SCHEDULE_READ"),
    "SalesSupport" to listOf("DASHBOARD_READ", "EMPLOYEE_READ", "ACCOUNT_READ", "SCHEDULE_READ"),
    "notice" to listOf("DASHBOARD_READ"),
    "ProfessionalPromotionTeam" to listOf("PROMOTION_READ", "PROMOTION_WRITE"),
)

/**
 * user.profile_type picklist 정정.
 *
 * DB 저장 형식: `ProfileTypeConverter` 가 `ProfileType.value` (SF raw 값, 예: "5.영업사원") 으로 저장.
 * V169 마이그레이션이 backend enum.name → SF raw 값 일괄 정정한 이력 (Spec #759/#760 hotfix) 와 정합.
 *
 * 결과값은 모두 `ProfileType.value` (운영 raw 값 형식, 숫자prefix 포함 한글) 로 통일.
 * 매핑 키는 SF Profile.Name 의 운영 변형 (공백 포함 / 정규화 / i18n) 까지 커버.
 */
internal val PROFILE_NAME_TO_PROFILE_TYPE: Map<String, String> = mapOf(
    // MARKETING — ProfileType.value = "8.마케팅"
    "8. 마케팅" to "8.마케팅",
    "8.마케팅" to "8.마케팅",
    "마케팅" to "8.마케팅",
    // STAFF — ProfileType.value = "9. Staff"
    "9. Staff" to "9. Staff",
    "9.Staff" to "9. Staff",
    "Staff" to "9. Staff",
    // TEAM_LEADER — ProfileType.value = "6.조장"
    "6. 조장" to "6.조장",
    "6.조장" to "6.조장",
    "조장" to "6.조장",
    // BRANCH_MANAGER — ProfileType.value = "4.지점장"
    "4. 지점장" to "4.지점장",
    "4.지점장" to "4.지점장",
    "지점장" to "4.지점장",
    // SALES_MANAGER — ProfileType.value = "3.영업부장"
    "3. 영업부장" to "3.영업부장",
    "3.영업부장" to "3.영업부장",
    "영업부장" to "3.영업부장",
    // BUSINESS_DIRECTOR — ProfileType.value = "2.사업부장"
    "2. 사업부장" to "2.사업부장",
    "2.사업부장" to "2.사업부장",
    "사업부장" to "2.사업부장",
    // DIVISION_HEAD — ProfileType.value = "1.본부장"
    "1. 본부장" to "1.본부장",
    "1.본부장" to "1.본부장",
    "본부장" to "1.본부장",
    // SALES_REP — ProfileType.value = "5.영업사원"
    "5. 영업사원" to "5.영업사원",
    "5.영업사원" to "5.영업사원",
    "영업사원" to "5.영업사원",
    // SYSTEM_ADMIN — ProfileType.value = "시스템 관리자"
    "시스템 관리자" to "시스템 관리자",
    "System Administrator" to "시스템 관리자",
    "システム管理者" to "시스템 관리자",
    "SYSTEM_ADMIN" to "시스템 관리자",
)
internal const val PROFILE_TYPE_FALLBACK = "9. Staff"
