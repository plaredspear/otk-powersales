-- OrderRequestProduct sf-meta-diff 정합 (Spec #761):
--
-- (1) OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename + owner_group_id (Group FK) 신규.
--     CHECK XOR `chk_order_request_product_owner_xor` 으로 둘 다 채움 금지.
-- (2) audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 `referenceTo = [User]` 정합).
-- (3) 타입·길이 정합 9건:
--     unit VARCHAR(10) → VARCHAR(40), product_code VARCHAR(20) → VARCHAR(255),
--     box_quantity NUMERIC(15,3) → NUMERIC(18,3), amount NUMERIC(16,2) → NUMERIC(18,2),
--     quantity_boxes NUMERIC(16,2) → NUMERIC(18,2),
--     line_number INTEGER → BIGINT, quantity_pieces INTEGER → BIGINT,
--     dk_total_count DOUBLE PRECISION → NUMERIC(18,0), total_count DOUBLE PRECISION → NUMERIC(18,0).
-- (4) LineChangeType — `is_cancelled` Boolean 우회 매핑 폐기, `line_change_type VARCHAR(10)` 신규.
--     SF `DKRetail__LineChangeType__c` type=string(10) free-form 정합. 운영 도메인 값 `{null, 'X'}`.
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id → User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     sf-migrate Phase 2 lookup (`<관계>_sfid` → `user.sfid` / `group.sfid` → 로컬 PK) 으로 FK 자동 채움.
--   - is_cancelled (Boolean) → line_change_type (String) 변환: SF 원본 string 복원 불가 → NULL 채움.
--     실제 운영 데이터는 sf-migrate 가 SF 원본을 다시 적재할 때 채워짐.
--
-- 관련: V80__sf_align_order_request_product.sql (audit FK 도입), V128__sf_align_order_request_owner_polymorphic_audit_user_fk_precision.sql (동일 패턴).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 제거
ALTER TABLE powersales.order_request_product
    DROP CONSTRAINT fk_order_request_product_owner;

-- 기존 컬럼 인덱스 제거 (RENAME 전에 정리)
DROP INDEX powersales.idx_order_request_product_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.order_request_product
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.order_request_product SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.order_request_product
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.order_request_product
    ADD CONSTRAINT fk_order_request_product_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_order_request_product_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.order_request_product
    ADD CONSTRAINT chk_order_request_product_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_order_request_product_owner_user_id  ON powersales.order_request_product (owner_user_id);
CREATE INDEX idx_order_request_product_owner_group_id ON powersales.order_request_product (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.order_request_product
    DROP CONSTRAINT fk_order_request_product_created_by,
    DROP CONSTRAINT fk_order_request_product_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.order_request_product
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.order_request_product
    ADD CONSTRAINT fk_order_request_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_order_request_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V80 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (3) 타입·길이 정합
-- ============================================================================

ALTER TABLE powersales.order_request_product
    ALTER COLUMN unit           TYPE VARCHAR(40),
    ALTER COLUMN product_code   TYPE VARCHAR(255),
    ALTER COLUMN box_quantity   TYPE NUMERIC(18, 3),
    ALTER COLUMN amount         TYPE NUMERIC(18, 2),
    ALTER COLUMN quantity_boxes TYPE NUMERIC(18, 2),
    ALTER COLUMN line_number    TYPE BIGINT,
    ALTER COLUMN quantity_pieces TYPE BIGINT,
    ALTER COLUMN dk_total_count TYPE NUMERIC(18, 0) USING dk_total_count::NUMERIC(18, 0),
    ALTER COLUMN total_count    TYPE NUMERIC(18, 0) USING total_count::NUMERIC(18, 0);

-- ============================================================================
-- (4) LineChangeType — is_cancelled (Boolean) → line_change_type (String)
-- ============================================================================

ALTER TABLE powersales.order_request_product
    ADD COLUMN line_change_type VARCHAR(10) NULL;

-- 기존 is_cancelled 값은 SF 원본 string 으로 복원 불가 (Boolean → string 손실 정보 미보유).
-- NULL 로 두고 sf-migrate 가 SF 원본 재적재 시 채움.

ALTER TABLE powersales.order_request_product
    DROP COLUMN is_cancelled;
