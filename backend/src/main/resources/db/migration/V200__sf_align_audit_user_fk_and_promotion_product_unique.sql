-- audit FK 일괄 정합 — 6 entity 의 created_by_id / last_modified_by_id 타입 Employee → User
-- + promotion_product.uk_promotion_product_promotion_id UNIQUE 제거 (SF Master-Detail 정합)
--
-- 배경 (audit FK):
--   Stage2 SF Resolve 가 created_by_sfid → user.sfid → user.user_id 를 created_by_id 컬럼에 박는데,
--   현재 FK 가 employee.employee_id 참조라 타입 mismatch → FK violation 발생.
--   예: "Key (created_by_id)=(66) is not present in table employee".
--
-- SF 권위 출처 (describe API dump):
--   inspection_theme(Theme__c) / new_product(NewProduct__c) / product_barcode(ProductBarcode__c) /
--   push_message(PushMessage__c) / upload_file(UploadFile__c) / holiday_master(HolidayMaster__c)
--   모두 CreatedById.referenceTo = [User], LastModifiedById.referenceTo = [User].
--
-- 패턴 (V116 account_category_master 패턴 동일):
--   (a) Employee FK 제약 제거
--   (b) Employee.employee_id 잔여값 NULL 초기화 (User.user_id 와 의미 불일치)
--   (c) User FK 재추가
--   (d) audit 인덱스는 컬럼명 변동 없어 유지 (재생성 불요)
--
-- 배경 (promotion_product UNIQUE):
--   V159 의 uk_promotion_product_promotion_id UNIQUE 가 SF
--   DKRetail__PromotionProduct__c.DKRetail__PromotionId__c 의 Master-Detail (relationshipOrder=0,
--   unique=False) 와 충돌. SF 운영은 한 Promotion 에 PromotionProduct 다수 허용 → 제약 제거.

BEGIN;

-- ============================================================================
-- (1) inspection_theme — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.inspection_theme
    DROP CONSTRAINT IF EXISTS fk_inspection_theme_created_by,
    DROP CONSTRAINT IF EXISTS fk_inspection_theme_last_modified_by;

UPDATE powersales.inspection_theme
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.inspection_theme
    ADD CONSTRAINT fk_inspection_theme_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_inspection_theme_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (2) new_product — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.new_product
    DROP CONSTRAINT IF EXISTS fk_new_product_created_by,
    DROP CONSTRAINT IF EXISTS fk_new_product_last_modified_by;

UPDATE powersales.new_product
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.new_product
    ADD CONSTRAINT fk_new_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_new_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (3) product_barcode — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.product_barcode
    DROP CONSTRAINT IF EXISTS fk_product_barcode_created_by,
    DROP CONSTRAINT IF EXISTS fk_product_barcode_last_modified_by;

UPDATE powersales.product_barcode
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.product_barcode
    ADD CONSTRAINT fk_product_barcode_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_product_barcode_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (4) push_message — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.push_message
    DROP CONSTRAINT IF EXISTS fk_push_message_created_by,
    DROP CONSTRAINT IF EXISTS fk_push_message_last_modified_by;

UPDATE powersales.push_message
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.push_message
    ADD CONSTRAINT fk_push_message_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_push_message_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (5) upload_file — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.upload_file
    DROP CONSTRAINT IF EXISTS fk_upload_file_created_by,
    DROP CONSTRAINT IF EXISTS fk_upload_file_last_modified_by;

UPDATE powersales.upload_file
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.upload_file
    ADD CONSTRAINT fk_upload_file_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_upload_file_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (6) holiday_master — audit FK Employee → User
-- ============================================================================

ALTER TABLE powersales.holiday_master
    DROP CONSTRAINT IF EXISTS fk_holiday_master_created_by,
    DROP CONSTRAINT IF EXISTS fk_holiday_master_last_modified_by;

UPDATE powersales.holiday_master
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.holiday_master
    ADD CONSTRAINT fk_holiday_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_holiday_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (7) promotion_product.uk_promotion_product_promotion_id 제거
--     SF DKRetail__PromotionProduct__c.DKRetail__PromotionId__c = Master-Detail child
--     (relationshipOrder=0, unique=False) → 한 Promotion 에 다수 child 허용 정합
-- ============================================================================

ALTER TABLE powersales.promotion_product
    DROP CONSTRAINT IF EXISTS uk_promotion_product_promotion_id;

-- promotion_id 는 lookup 빈번하므로 일반 인덱스로 대체 (UNIQUE 가 사라지면서 자동 인덱스도 사라짐)
CREATE INDEX IF NOT EXISTS idx_promotion_product_promotion_id
    ON powersales.promotion_product (promotion_id);

COMMIT;
