-- spec #808 — PermissionSet 의 가상 자원 (Custom Permission) 권한 비트 컬럼 추가.
--
-- `@PermissionResource("dashboard")` 같은 JPA entity 가 없는 가상 자원의 CRUD 비트 운반.
-- 구조 예시: { "dashboard": { "allowRead": true }, "report_monthly": { "allowRead": true } }
--
-- SF metadata 의 PermissionSet.customPermissions (CustomPermission 메타) 와 1:1 대응.

ALTER TABLE powersales.permission_set_flags
    ADD COLUMN custom_permissions JSONB;

COMMENT ON COLUMN powersales.permission_set_flags.custom_permissions IS
    '{ "dashboard": { "allowRead": true, "allowCreate": false, "allowEdit": false, "allowDelete": false }, ... } — JPA entity 가 없는 가상 자원 (@PermissionResource) 의 CRUD 비트. SF customPermissions 정합.';
