-- Spec #804 — permission_set_assignment 의 audit 컬럼 + active 부여 1개 invariant.
--
-- 1. audit 컬럼 추가 (created_at / updated_at / created_by_id / updated_by_id)
--    - created_at / updated_at: 기존 BaseEntity 패턴과 정합. 본 spec 으로 PermissionSetAssignment 가 BaseEntity 상속 전환.
--    - created_by_id / updated_by_id: User 의 단순 FK (운영 감사 trail). nullable — Stage1 cut-over 적재분은 시스템 계정 부재.
-- 2. partial UNIQUE 인덱스 — 동일 (user, ps) active row 가 동시에 2개 이상 존재하지 않음을 DB 가 보장.
--    cut-over 적재 시점은 SF org 측에서 이미 unique 보장돼 충돌 없음. 운영 중 부여 endpoint 의 race 만 가드.

ALTER TABLE powersales.permission_set_assignment
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_permission_set_assignment_active_unique
    ON powersales.permission_set_assignment (assignee_user_id, permission_set_flags_id)
    WHERE is_active = TRUE;
