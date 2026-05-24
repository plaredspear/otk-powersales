-- permission_set_assignment.assignee_user_sfid NOT NULL → NULL 허용
--
-- 배경:
--   sfid 는 SF 데이터 마이그레이션 보조 필드이며 신규 시스템 service 로직에서 활용 금지.
--   web admin runtime 신규 PSA 부여 시 sfid 박는 로직 제거 (AdminPermissionAssignmentService).
--   따라서 assignee_user_sfid / permission_set_sfid 모두 runtime 신규 row 는 NULL 일 수 있음.
--
--   V175 의 NOT NULL 제약이 Stage1 적재만 보장하는 운영 invariant 였으나
--   web admin 신규 부여 row 가 NULL 박을 수 있어야 하므로 제약 해제.
--
-- partial UNIQUE 인덱스 (V183 idx_permission_set_assignment_natural_key_unique) 는 이미
-- `WHERE permission_set_sfid IS NOT NULL` 조건이라 NULL 다수 row 허용 — 영향 없음.
--
-- partial UNIQUE 인덱스 (V187 idx_permission_set_assignment_active_unique) 는
-- (assignee_user_id, permission_set_flags_id) WHERE is_active=TRUE 기준이라 sfid 와 무관.

ALTER TABLE powersales.permission_set_assignment
    ALTER COLUMN assignee_user_sfid DROP NOT NULL;
