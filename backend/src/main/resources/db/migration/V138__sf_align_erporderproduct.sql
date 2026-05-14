-- ErpOrderProduct sf-meta-diff 정합 (prod/diff/ERP_OrderProduct__c.md Q1~Q8):
--
-- (1) OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename + owner_group_id (Group FK) 신규.
--     CHECK XOR `chk_erp_order_product_owner_xor` 으로 둘 다 채움 금지.
-- (2) audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 `referenceTo = [User]` 정합).
-- (3) 타입·길이 정합:
--     Q4 string 절단 위험 10건 — SF length 까지 확장.
--     Q5 double scale=0 정수 도메인 6건 — DOUBLE PRECISION → BIGINT.
--     Q6 double scale>0 소수점 정밀도 4건 — DOUBLE PRECISION / NUMERIC(18,0) → NUMERIC(18, scale).
-- (4) Q7 Formula 컬럼 제거 — order_date (Formula `ERPOrderId__r.OrderDate__c`) DROP. §6.7 (컬럼 추가 금지) 정합.
-- (5) Q8 string 좁힘 4건 — entity 길이 SF 정합으로 좁힘 (절단 위험 없음, 메타 일관성).
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id → User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 보존 — sf-migrate Phase 2 lookup
--     (`<관계>_sfid` → `user.sfid` / `group.sfid` → 로컬 PK) 으로 FK 자동 채움.
--   - Double → BIGINT 전환은 소수점 잔존값이 있을 경우 반올림 (TRUNC). 운영 데이터 정수성 검증은 #759 본 작업 사용자 확정 ("모두 권고로 결정") 기준 적용.
--
-- 관련: V78__sf_align_erp_order_product.sql (audit/owner 도입), V134__sf_align_orderrequestproduct_owner_polymorphic.sql (동일 패턴).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    DROP CONSTRAINT fk_erp_order_product_owner;

DROP INDEX powersales.idx_erp_order_product_owner_id;

ALTER TABLE powersales.erp_order_product
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.erp_order_product SET owner_user_id = NULL;

ALTER TABLE powersales.erp_order_product
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.erp_order_product
    ADD CONSTRAINT fk_erp_order_product_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_erp_order_product_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

ALTER TABLE powersales.erp_order_product
    ADD CONSTRAINT chk_erp_order_product_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_erp_order_product_owner_user_id  ON powersales.erp_order_product (owner_user_id);
CREATE INDEX idx_erp_order_product_owner_group_id ON powersales.erp_order_product (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    DROP CONSTRAINT fk_erp_order_product_created_by,
    DROP CONSTRAINT fk_erp_order_product_last_modified_by;

UPDATE powersales.erp_order_product
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.erp_order_product
    ADD CONSTRAINT fk_erp_order_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_erp_order_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V78 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (3) Q4 string 절단 위험 — entity length < SF length 확장 (10건)
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    ALTER COLUMN confirm_unit           TYPE VARCHAR(255),
    ALTER COLUMN external_key           TYPE VARCHAR(255),
    ALTER COLUMN line_item_status       TYPE VARCHAR(40),
    ALTER COLUMN line_number            TYPE VARCHAR(255),
    ALTER COLUMN delivery_status        TYPE VARCHAR(30),
    ALTER COLUMN product_code           TYPE VARCHAR(100),
    ALTER COLUMN shipping_complete_time TYPE VARCHAR(30),
    ALTER COLUMN shipping_driver_phone  TYPE VARCHAR(40),
    ALTER COLUMN shipping_schedule_time TYPE VARCHAR(30),
    ALTER COLUMN shipping_vehicle       TYPE VARCHAR(30);

-- ============================================================================
-- (4) Q5 double scale=0 정수 도메인 — DOUBLE PRECISION → BIGINT (6건)
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    ALTER COLUMN order_quantity         TYPE BIGINT USING ROUND(order_quantity)::BIGINT,
    ALTER COLUMN order_sales_line_amount TYPE BIGINT USING ROUND(order_sales_line_amount)::BIGINT,
    ALTER COLUMN release_amount         TYPE BIGINT USING ROUND(release_amount)::BIGINT,
    ALTER COLUMN release_quantity       TYPE BIGINT USING ROUND(release_quantity)::BIGINT,
    ALTER COLUMN shipping_amount        TYPE BIGINT USING ROUND(shipping_amount)::BIGINT,
    ALTER COLUMN shipping_quantity      TYPE BIGINT USING ROUND(shipping_quantity)::BIGINT;

-- ============================================================================
-- (5) Q6 double scale>0 소수점 정밀도 — NUMERIC(18, scale) 정합 (4건)
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    ALTER COLUMN box_quantity         TYPE NUMERIC(18, 4) USING box_quantity::NUMERIC(18, 4),
    ALTER COLUMN confirm_quantity_box TYPE NUMERIC(18, 4) USING confirm_quantity_box::NUMERIC(18, 4),
    ALTER COLUMN confirm_quantity     TYPE NUMERIC(18, 3) USING confirm_quantity::NUMERIC(18, 3),
    ALTER COLUMN shipping_quantity_box TYPE NUMERIC(18, 2) USING shipping_quantity_box::NUMERIC(18, 2);

-- ============================================================================
-- (6) Q7 Formula 컬럼 제거 — order_date (Formula `ERPOrderId__r.OrderDate__c`) DROP
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    DROP COLUMN order_date;

-- ============================================================================
-- (7) Q8 string 좁힘 — entity length > SF length (절단 위험 없음, SF 정합) (4건)
-- ============================================================================

ALTER TABLE powersales.erp_order_product
    ALTER COLUMN default_reason       TYPE VARCHAR(50),
    ALTER COLUMN plant                TYPE VARCHAR(4),
    ALTER COLUMN plant_nm             TYPE VARCHAR(30),
    ALTER COLUMN shipping_driver_name TYPE VARCHAR(30);
