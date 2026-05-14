-- Claim sf-meta-diff 정합 (Q2~Q7):
--
-- (1) Q2: OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename + owner_group_id (Group FK) 신규.
--     CHECK XOR `chk_claim_owner_xor` 으로 둘 다 채움 금지.
-- (2) Q3/Q4: audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 `referenceTo = [User]` 정합).
-- (3) Q5/Q6/Q7: SF 절단 위험 정합 — name (50→80), action_code (20→40), counsel_number (30→40).
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id → User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     sf-migrate Phase 2 lookup (`<관계>_sfid` → `user.sfid` / `group.sfid` → 로컬 PK) 으로 FK 자동 채움.
--
-- 관련: V60__sf_align_claim.sql (#705, Employee FK 도입), V116__sf_align_account_category_master_owner_polymorphic_and_user_fk.sql (#755/#758 패턴 선례).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 제거
ALTER TABLE powersales.claim
    DROP CONSTRAINT fk_claim_owner;

-- 기존 컬럼 인덱스 제거 (RENAME 전에 정리)
DROP INDEX powersales.idx_claim_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.claim
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.claim SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.claim
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.claim
    ADD CONSTRAINT fk_claim_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_claim_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.claim
    ADD CONSTRAINT chk_claim_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_claim_owner_user_id  ON powersales.claim (owner_user_id);
CREATE INDEX idx_claim_owner_group_id ON powersales.claim (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.claim
    DROP CONSTRAINT fk_claim_created_by,
    DROP CONSTRAINT fk_claim_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.claim
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.claim
    ADD CONSTRAINT fk_claim_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_claim_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V60 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (3) SF 절단 위험 정합 — length 보정
-- ============================================================================

ALTER TABLE powersales.claim
    ALTER COLUMN name          TYPE VARCHAR(80),
    ALTER COLUMN action_code   TYPE VARCHAR(40),
    ALTER COLUMN counsel_number TYPE VARCHAR(40);
