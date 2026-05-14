-- AccountCategoryMaster.OwnerId polymorphic R-2 + audit FK Employee → User 일괄 전환
--
-- (1) Spec #755 정합 — SF `AccountCategoryMaster__c.OwnerId.referenceTo = [Group, User]` polymorphic.
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename 하고 owner_group_id (Group FK) 신규 추가.
--     CHECK XOR `chk_account_category_master_owner_xor` 으로 둘 다 채움 금지.
-- (2) Spec #758 정합 — created_by_id / last_modified_by_id FK 대상 Employee → User 전환.
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id 였으므로
--     User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - 운영에서는 owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     SalesforceMigrationTool Phase 2 lookup (`<관계>_sfid` → `user.sfid` → `user.user_id`)
--     으로 FK 가 자동 채워진다.
--
-- 관련: V59__sf_align_account_category_master.sql (#704, Employee FK 도입), V112__sf_align_appointment_owner_group.sql (#755 패턴 선례).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id 를 owner_user_id 로 rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 제거
ALTER TABLE powersales.account_category_master
    DROP CONSTRAINT fk_account_category_master_owner;

-- 기존 컬럼 인덱스 제거 (RENAME 전에 정리 — 새 컬럼명으로 재생성)
DROP INDEX powersales.idx_account_category_master_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.account_category_master
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.account_category_master SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.account_category_master
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.account_category_master
    ADD CONSTRAINT fk_account_category_master_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_category_master_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.account_category_master
    ADD CONSTRAINT chk_account_category_master_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_account_category_master_owner_user_id  ON powersales.account_category_master (owner_user_id);
CREATE INDEX idx_account_category_master_owner_group_id ON powersales.account_category_master (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.account_category_master
    DROP CONSTRAINT fk_account_category_master_created_by,
    DROP CONSTRAINT fk_account_category_master_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.account_category_master
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.account_category_master
    ADD CONSTRAINT fk_account_category_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_category_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 기존 (V59) 유지 — 컬럼명 변동 없음.
