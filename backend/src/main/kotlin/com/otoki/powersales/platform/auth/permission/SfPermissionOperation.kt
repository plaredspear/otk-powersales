package com.otoki.powersales.platform.auth.permission

/**
 * SF 권한 모델의 CRUD operation 또는 system 비트 분기.
 *
 * - `READ` / `CREATE` / `EDIT` / `DELETE`: SF PermissionSetFlags.object_permissions JSON 의
 *   `allowRead` / `allowCreate` / `allowEdit` / `allowDelete` 비트 1:1 매핑.
 * - `SYSTEM`: ProfileFlags / PermissionSetFlags 의 system 권한 비트 매칭 (어노테이션의
 *   `systemPermission` 속성과 함께 사용).
 *
 * SF 정합 출처: `permissionsets/<Name>.permissionset-meta.xml` 의 `<objectPermissions>` element.
 */
enum class SfPermissionOperation {
    READ,
    CREATE,
    EDIT,
    DELETE,
    SYSTEM,
}
