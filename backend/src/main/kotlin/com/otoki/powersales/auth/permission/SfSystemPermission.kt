package com.otoki.powersales.auth.permission

/**
 * SF Profile / PermissionSet 의 system 권한 비트 화이트리스트.
 *
 * 각 enum 항목은 ProfileFlags / PermissionSetFlags 의 boolean 컬럼과 1:1 매핑한다.
 * `@RequiresSfPermission(operation = SYSTEM, systemPermission = X)` 형식으로 사용.
 *
 * SF 정합 출처: `profiles/<Name>.profile-meta.xml` 의 `<userPermissions>` element +
 * `permissionsets/<Name>.permissionset-meta.xml` 의 system 비트.
 */
enum class SfSystemPermission(val columnName: String) {
    /** 모든 entity 의 READ 통과 — SF 표준 동작. */
    VIEW_ALL_DATA("permissions_view_all_data"),

    /** 모든 entity 의 모든 CRUD 통과 — SF 표준 동작. */
    MODIFY_ALL_DATA("permissions_modify_all_data"),

    /** User entity 전체 가시성. */
    VIEW_ALL_USERS("permissions_view_all_users"),

    /** User CRUD 권한. */
    MANAGE_USERS("permissions_manage_users"),

    /** API 접근 가능. */
    API_ENABLED("permissions_api_enabled"),
}
