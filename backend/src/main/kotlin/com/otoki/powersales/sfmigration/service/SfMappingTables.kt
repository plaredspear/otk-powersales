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

internal val PPT_KOREAN_TO_ENUM: Map<String, String> = mapOf(
    "라면세일조" to "RAMEN_SALE",
    "프레시세일조_냉장" to "FRESH_SALE_REFRIGERATED",
    "프레시세일조_냉동" to "FRESH_SALE_FROZEN",
    "프레시세일조_만두" to "FRESH_SALE_DUMPLING",
    "카레행사조" to "CURRY_PROMOTION",
)

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

internal val PROFILE_NAME_TO_PROFILE_TYPE: Map<String, String> = mapOf(
    "8. 마케팅" to "MARKETING",
    "마케팅" to "MARKETING",
    "9. Staff" to "STAFF",
    "Staff" to "STAFF",
    "6. 조장" to "TEAM_LEADER",
    "조장" to "TEAM_LEADER",
    "4. 지점장" to "BRANCH_MANAGER",
    "지점장" to "BRANCH_MANAGER",
    "영업부장" to "SALES_MANAGER",
    "사업부장" to "BUSINESS_DIRECTOR",
    "본부장" to "DIVISION_HEAD",
    "5. 영업사원" to "SALES_REP",
    "영업사원" to "SALES_REP",
    "시스템 관리자" to "SYSTEM_ADMIN",
    "System Administrator" to "SYSTEM_ADMIN",
    "システム管理者" to "SYSTEM_ADMIN",
)
internal const val PROFILE_TYPE_FALLBACK = "STAFF"
