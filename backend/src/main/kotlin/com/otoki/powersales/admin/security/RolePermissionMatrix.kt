package com.otoki.powersales.admin.security

import com.otoki.powersales.auth.entity.UserRole

/**
 * 운영 역할 × 권한 매트릭스 SoT (Single source of truth).
 *
 * 부팅 시 [com.otoki.powersales.admin.service.RolePermissionSyncRunner] 가
 * 본 매트릭스를 `role_permission` 테이블과 INSERT-only 로 동기화한다.
 * DB-only row 는 보존되어 web admin 의 권한 매트릭스 수정 UI 가 추가한 권한이 유지된다.
 *
 * 신규 권한 추가 절차:
 * 1. [AdminPermission] enum 에 항목 추가
 * 2. 본 [MATRIX] 의 각 운영 역할 Set 에 부여 여부 결정 (부여하지 않더라도 모든 권한이
 *    `ALL_KNOWN_PERMISSIONS` 검사를 통과해야 함 — 어디든 한 곳 이상 포함되어야 한다)
 *
 * 정책 출처: V2 (8개 역할 베이스라인) + V19 / V50 / V51 / V52 / V56 / V150
 * (SYSTEM_ADMIN 단독 추가 권한들) + V51 의 SALES_SUPPORT ACCOUNT_WRITE.
 */
object RolePermissionMatrix {

    val MATRIX: Map<UserRole, Set<AdminPermission>> = mapOf(
        UserRole.SYSTEM_ADMIN to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.EMPLOYEE_WRITE,
            AdminPermission.EMPLOYEE_RESET_CREDENTIALS,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.ACCOUNT_WRITE,
            AdminPermission.ACCOUNT_DELETE,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SCHEDULE_WRITE,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.NAVER_GEOCODE_TEST,
            AdminPermission.AGREEMENT_READ,
            AdminPermission.AGREEMENT_WRITE,
            AdminPermission.USER_READ,
            AdminPermission.USER_WRITE,
            AdminPermission.SCHEDULED_JOB_READ,
            AdminPermission.EMPLOYEE_INPUT_CRITERIA_READ,
            AdminPermission.EMPLOYEE_INPUT_CRITERIA_WRITE,
            AdminPermission.SAP_INTEGRATION_READ,
            AdminPermission.ATTEND_INFO_READ,
            AdminPermission.ATTEND_INFO_WRITE,
            AdminPermission.ATTEND_INFO_DELETE,
            AdminPermission.ATTENDANCE_LOG_READ,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.SALES_SUPPORT to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.EMPLOYEE_WRITE,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.ACCOUNT_WRITE,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SCHEDULE_WRITE,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.EMPLOYEE_INPUT_CRITERIA_READ,
            AdminPermission.EMPLOYEE_INPUT_CRITERIA_WRITE,
            AdminPermission.ATTEND_INFO_READ,
            AdminPermission.ATTEND_INFO_WRITE,
            AdminPermission.ATTENDANCE_LOG_READ,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.LEADER to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SCHEDULE_WRITE,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.BRANCH_MANAGER to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.SALES_MANAGER to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.BUSINESS_MANAGER to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
        UserRole.HEADQUARTERS_MANAGER to setOf(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.EMPLOYEE_READ,
            AdminPermission.ACCOUNT_READ,
            AdminPermission.PROMOTION_READ,
            AdminPermission.PROMOTION_WRITE,
            AdminPermission.SAFETY_CHECK_READ,
            AdminPermission.SCHEDULE_READ,
            AdminPermission.SALES_COMPARISON_READ,
            AdminPermission.MONTHLY_INPUT_ADEQUACY_READ,
            AdminPermission.PRODUCT_EXPIRATION_READ,
            AdminPermission.PRODUCT_EXPIRATION_WRITE,
            AdminPermission.MONTHLY_SALES_DASHBOARD_READ,
        ),
    )

    init {
        val granted: Set<AdminPermission> = MATRIX.values.flatten().toSet()
        val missing: Set<AdminPermission> = AdminPermission.entries.toSet() - granted
        require(missing.isEmpty()) {
            "RolePermissionMatrix 누락 권한: $missing — 각 AdminPermission 은 최소 1개 운영 역할에 부여되어야 한다"
        }
    }

    fun asPairs(): List<Pair<UserRole, AdminPermission>> =
        MATRIX.flatMap { (role, perms) -> perms.map { role to it } }
}
