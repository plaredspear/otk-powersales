-- Organization sf-meta-diff 정합 (Q1~Q3):
--
-- (1) Q1: OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename + owner_group_id (Group FK) 신규.
--     CHECK XOR `chk_organization_owner_xor` 으로 둘 다 채움 금지.
-- (2) Q2/Q3: audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 `referenceTo = [User]` 정합).
--
-- 데이터 처리:
--   - Organization 은 SAP HR 동기화 시 DELETE_INSERT 되는 entity — 기존 PK 가 매 동기화마다 변경되므로
--     owner_id / created_by_id / last_modified_by_id 잔여값도 의미 정합 깨짐 → NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 보존 → sf-migrate Phase 2 가
--     `<관계>_sfid` → `user.sfid` / `group.sfid` lookup 으로 FK 자동 채움.
--
-- 관련: V71__sf_align_organization.sql (audit/owner Employee FK 도입),
--       V128__sf_align_order_request_owner_polymorphic_audit_user_fk_precision.sql (동일 패턴 선례).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 + 인덱스 제거
ALTER TABLE powersales.organization
    DROP CONSTRAINT fk_organization_owner;

DROP INDEX powersales.idx_organization_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.organization
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.organization SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.organization
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.organization
    ADD CONSTRAINT fk_organization_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_organization_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.organization
    ADD CONSTRAINT chk_organization_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_organization_owner_user_id  ON powersales.organization (owner_user_id);
CREATE INDEX idx_organization_owner_group_id ON powersales.organization (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.organization
    DROP CONSTRAINT fk_organization_created_by,
    DROP CONSTRAINT fk_organization_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.organization
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.organization
    ADD CONSTRAINT fk_organization_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_organization_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V71 에서 생성 — 컬럼명 변동 없음, 유지.
