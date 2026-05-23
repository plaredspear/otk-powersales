-- spec #798 — PermissionSetAssignment Stage1 정규 적재용 스키마 보강.
--
-- 변경 사항:
--   1. permission_set_sfid VARCHAR(18) 신규 컬럼 (Stage1 적재 시점 SF lookup sfid 박제)
--   2. permission_set_flags_id NOT NULL 제약 해제 (Stage1 적재 시 NULL, Stage2 fk substep 후 채움)
--   3. UNIQUE (assignee_user_sfid, permission_set_sfid) 추가 (#798 Q4 옵션 1 — 동일 부여 1건)
--
-- Stage 2 fk substep 동작:
--   permission_set_assignment.permission_set_sfid → permission_set_flags.permission_set_sfid lookup
--   → permission_set_flags_id 채움

ALTER TABLE powersales.permission_set_assignment
    ADD COLUMN permission_set_sfid VARCHAR(18);

ALTER TABLE powersales.permission_set_assignment
    ALTER COLUMN permission_set_flags_id DROP NOT NULL;

CREATE UNIQUE INDEX idx_permission_set_assignment_natural_key_unique
    ON powersales.permission_set_assignment (assignee_user_sfid, permission_set_sfid)
    WHERE permission_set_sfid IS NOT NULL;

COMMENT ON COLUMN powersales.permission_set_assignment.permission_set_sfid IS
    'SF PermissionSet sfid — Stage1 적재 시점 박제. Stage2 fk substep 이 permission_set_flags 측 sfid 로 lookup 하여 permission_set_flags_id 채움 (spec #798)';
