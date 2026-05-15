-- MonthlySalesHistory sf-meta-diff 정합 (Q1~Q6):
--
-- (Q1) OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--      기존 owner_id (Employee FK) → owner_user_id (User FK) rename + owner_group_id (Group FK) 신규.
--      XOR CHECK `chk_monthly_sales_history_owner_xor` 으로 둘 다 채움 금지.
-- (Q2) audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 referenceTo = [User] 정합).
-- (Q3) Picklist enum 정합: sales_year / sales_month 컬럼 길이 VARCHAR(255) → VARCHAR(4) / VARCHAR(2).
--      Kotlin 측은 enum (SalesYear / SalesMonth) + AttributeConverter 도입.
-- (Q4) currency 필드 Double → BigDecimal: last_month_target_by_hand / this_month_target / last_month_results
--      double precision → NUMERIC(18, 0). SF currency precision=18 scale=0 정합.
-- (Q5) Spec #575 번복: total_ledger_amount NUMERIC(18, 4) → NUMERIC(18, 0). SF prod 메타 (double scale=0) 정합.
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id → User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     sf-migrate Phase 2 lookup (`<관계>_sfid` → `user.sfid` / `group.sfid` → 로컬 PK) 으로 FK 자동 채움.
--   - total_ledger_amount scale 좁힘은 SAP 인바운드 적재 시 HALF_UP 반올림으로 절사 (application 코드 동시 갱신).
--
-- 관련: V88 (R-2 도입), V128/V127 등 (동일 polymorphic 패턴), V17 (total_ledger_amount 도입).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 제거
ALTER TABLE powersales.monthly_sales_history
    DROP CONSTRAINT fk_monthly_sales_history_owner;

-- 기존 컬럼 인덱스 제거 (RENAME 전에 정리)
DROP INDEX powersales.idx_monthly_sales_history_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.monthly_sales_history
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.monthly_sales_history SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.monthly_sales_history
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.monthly_sales_history
    ADD CONSTRAINT fk_monthly_sales_history_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_sales_history_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.monthly_sales_history
    ADD CONSTRAINT chk_monthly_sales_history_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_monthly_sales_history_owner_user_id  ON powersales.monthly_sales_history (owner_user_id);
CREATE INDEX idx_monthly_sales_history_owner_group_id ON powersales.monthly_sales_history (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.monthly_sales_history
    DROP CONSTRAINT fk_monthly_sales_history_created_by,
    DROP CONSTRAINT fk_monthly_sales_history_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.monthly_sales_history
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.monthly_sales_history
    ADD CONSTRAINT fk_monthly_sales_history_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_sales_history_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V88 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (3) Picklist 컬럼 길이 좁힘 — sales_year / sales_month
-- ============================================================================

ALTER TABLE powersales.monthly_sales_history
    ALTER COLUMN sales_year  TYPE VARCHAR(4) USING sales_year::VARCHAR(4),
    ALTER COLUMN sales_month TYPE VARCHAR(2) USING sales_month::VARCHAR(2);

-- ============================================================================
-- (4) currency 필드 NUMERIC(18, 0) 정합 — Double → BigDecimal
-- ============================================================================

ALTER TABLE powersales.monthly_sales_history
    ALTER COLUMN last_month_results         TYPE NUMERIC(18, 0) USING ROUND(last_month_results::NUMERIC, 0),
    ALTER COLUMN last_month_target_by_hand  TYPE NUMERIC(18, 0) USING ROUND(last_month_target_by_hand::NUMERIC, 0),
    ALTER COLUMN this_month_target          TYPE NUMERIC(18, 0) USING ROUND(this_month_target::NUMERIC, 0);

-- ============================================================================
-- (5) total_ledger_amount scale 좁힘 — NUMERIC(18, 4) → NUMERIC(18, 0)
-- ============================================================================

ALTER TABLE powersales.monthly_sales_history
    ALTER COLUMN total_ledger_amount TYPE NUMERIC(18, 0) USING ROUND(total_ledger_amount, 0);
