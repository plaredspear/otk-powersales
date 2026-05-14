-- DKRetail__Employee__c ↔ Employee SF 정합 (sf-meta-diff).
--
-- (1) OwnerId polymorphic R-2 — SF `referenceTo = [Group, User]` 의 SF 메타 권위 정합.
--     기존 owner_id (Employee FK) → owner_user_id (User FK) + owner_group_id (Group FK) + CHECK XOR.
--     sandbox/README.md §6.4 v2.6/v2.8 정책.
--
-- (2) Audit FK Employee → User 일괄 전환 (CreatedById / LastModifiedById).
--     SF `referenceTo = [User]` 의 정합 — sandbox/README.md §6.4 v2.7.
--     기존 잔여값은 Employee.employee_id 였으므로 User.user_id 와 의미 정합 깨짐
--     → NULL 초기화 후 sf-migrate Phase 2 lookup (`<관계>_sfid` → `user.sfid` → `user.user_id`).
--
-- (3) role 컬럼은 application 측 Converter 가 한글 SF 원본값 저장으로 전환 — 기존 영문 데이터 NULL 초기화.
--     §6.6 v2.2 — picklist DB 저장은 SF 원본 옵션값 보존.
--
-- 관련 선례: V112 (Appointment polymorphic), V115 (account audit User FK), V117 (Appointment audit User FK).

-- ============================================================================
-- (1) employee.owner — Employee FK → polymorphic [User, Group]
-- ============================================================================

-- 기존 Employee FK constraint + 인덱스 + 컬럼 제거
ALTER TABLE powersales.employee
    DROP CONSTRAINT IF EXISTS fk_employee_owner;

DROP INDEX IF EXISTS powersales.idx_employee_owner_id;

ALTER TABLE powersales.employee
    DROP COLUMN IF EXISTS owner_id;

-- polymorphic 컬럼 2개 추가
ALTER TABLE powersales.employee
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.employee
    ADD CONSTRAINT fk_employee_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.employee
    ADD CONSTRAINT chk_employee_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_employee_owner_user_id  ON powersales.employee (owner_user_id);
CREATE INDEX idx_employee_owner_group_id ON powersales.employee (owner_group_id);

-- ============================================================================
-- (2) employee.created_by_id / last_modified_by_id — Employee FK → User FK
-- ============================================================================

ALTER TABLE powersales.employee
    DROP CONSTRAINT IF EXISTS fk_employee_created_by,
    DROP CONSTRAINT IF EXISTS fk_employee_last_modified_by;

UPDATE powersales.employee
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.employee
    ADD CONSTRAINT fk_employee_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 기존 마이그레이션에서 생성된 것 유지 — 컬럼명 변동 없음.

-- ============================================================================
-- (3) employee.role — 영문 enum.name 저장 데이터 NULL 초기화
-- ============================================================================

-- §6.6 v2.2 정합 — Converter 변경으로 DB 는 SF 원본 한글값 (`조장` / `여사원` 등) 저장 전환.
-- 기존 영문 (`LEADER` / `WOMAN` 등) 데이터는 사용자 결정에 따라 삭제.
UPDATE powersales.employee
    SET role = NULL;
