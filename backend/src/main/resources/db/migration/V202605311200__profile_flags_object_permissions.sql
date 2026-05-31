-- profile_flags 에 객체/가상자원 권한 + dirty 플래그 추가.
--
-- 배경: SF 레거시에서 Profile 은 ObjectPermission (객체별 CRUD) 을 보유하며, 발령(인사이동) 시
--   User.ProfileId 가 직책 기반으로 자동 결정되어 "직책 → 화면 권한" 이 Profile 로 자동 전파된다
--   (AppointmentTriggerHanlder 의 ProfileId 10분기). 반면 PermissionSet 은 발령과 무관한 수동 부여.
--   신규 시스템 초기 마이그레이션 (V175) 에서 profile_flags 는 system 비트 5종만 보존하고 객체권한을
--   PermissionSetFlags 로만 몰아넣었으나, 이는 "조장이면 화면권한 자동" 의 SF 자동 연결고리(Profile)를
--   끊는다. 본 마이그레이션으로 Profile 의 객체권한 슬롯을 복원하여 SF 정합 + 직책 자동 권한을 회복한다.
--
-- 컬럼:
--   - object_permissions : SF API name → {allowRead, allowCreate, allowEdit, allowDelete}. PermissionSetFlags 와 동일 구조.
--   - custom_permissions : 가상 자원 (@PermissionResource) → 동일 4비트. SF API name 아님.
--   - is_locally_modified: Web admin 에서 권한 비트를 편집하면 TRUE. Stage2 재적재 시 dirty row 는 skip (SF 덮어쓰기 보호).
ALTER TABLE powersales.profile_flags
    ADD COLUMN object_permissions  JSONB,
    ADD COLUMN custom_permissions  JSONB,
    ADD COLUMN is_locally_modified BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN powersales.profile_flags.object_permissions IS
    'SF Profile 객체권한. { "MonthlySalesHistory__c": { "allowRead": true }, ... }. PermissionSetFlags.object_permissions 와 동일 구조.';

COMMENT ON COLUMN powersales.profile_flags.custom_permissions IS
    '가상 자원 (@PermissionResource) 권한 비트. { "dashboard": { "allowRead": true }, ... }.';

COMMENT ON COLUMN powersales.profile_flags.is_locally_modified IS
    '신규 시스템 (Web admin) 에서 Profile 권한 비트가 수정되면 TRUE. Stage2 재적재 시 보호 판단.';
