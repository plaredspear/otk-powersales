-- user_permission 권한 모델을 User 기반으로 전환
--
-- 이전:
--   user_permission.employee_id BIGINT NOT NULL → employee(employee_id)
--   user_permission.granted_by  BIGINT NOT NULL → employee(employee_id)
--   UNIQUE (employee_id, permission)
--
-- 이후:
--   user_permission.user_id BIGINT NOT NULL → "user"(user_id)
--   granted_by 컬럼 삭제 (레거시 SF 대비 권한 부여자 정보를 별도 관리하지 않음)
--   UNIQUE (user_id, permission)
--
-- 배경:
--  - 인증/관리 시스템이 User 기반으로 통일됨.
--  - 권한 부여자(granted_by) 는 audit 용 메타데이터로 본 시스템 정책상 보관 불필요.
--  - SF cut-over (Spec #764) 시 부여자 컬럼이 없으면 단순화됨.
--
-- 사전 조건:
--  - 본 마이그레이션 시점에 user_permission 의 모든 employee_id 가 동일 employee_code 의
--    user 와 1:1 매칭 가능해야 함 (employee.employee_code = user.employee_code).

BEGIN;

-- 1) user_id 컬럼 추가 (NULL 허용 상태로 시작)
ALTER TABLE powersales.user_permission
    ADD COLUMN user_id BIGINT;

-- 2) employee_id → user_id 매핑 (employee_code 기반 JOIN)
UPDATE powersales.user_permission up
SET user_id = u.user_id
FROM powersales.employee e
JOIN powersales."user" u ON u.employee_code = e.employee_code
WHERE up.employee_id = e.employee_id;

-- 3) 매핑 실패 row 정리 — 매칭되는 user 가 없는 권한은 의미 없음
DELETE FROM powersales.user_permission WHERE user_id IS NULL;

-- 4) NOT NULL 제약 + FK 신설
ALTER TABLE powersales.user_permission
    ALTER COLUMN user_id SET NOT NULL,
    ADD CONSTRAINT fk_user_permission_user
        FOREIGN KEY (user_id) REFERENCES powersales."user" (user_id);

-- 5) 기존 employee_id 제약/인덱스/컬럼 제거
ALTER TABLE powersales.user_permission
    DROP CONSTRAINT IF EXISTS uq_user_permission;

ALTER TABLE powersales.user_permission
    DROP CONSTRAINT IF EXISTS fk_user_permission_employee;

ALTER TABLE powersales.user_permission
    DROP COLUMN employee_id;

-- 6) granted_by 제약/인덱스/컬럼 제거
DROP INDEX IF EXISTS powersales.idx_user_permission_granted_by;

ALTER TABLE powersales.user_permission
    DROP CONSTRAINT IF EXISTS fk_user_permission_granted_by;

ALTER TABLE powersales.user_permission
    DROP COLUMN granted_by;

-- 7) 새 UNIQUE 제약 + FK 인덱스
ALTER TABLE powersales.user_permission
    ADD CONSTRAINT uq_user_permission UNIQUE (user_id, permission);

CREATE INDEX idx_user_permission_user_id ON powersales.user_permission (user_id);

COMMIT;
