-- owner FK 일괄 정합 — owner_id (Employee) → owner_user_id (User) + Group polymorphic 추가
-- + NewProduct.record_type_id (RecordType FK) 추가
--
-- 배경:
--   Stage2 SF Resolve (SfMigrationStage2FkService) 가 *_sfid → 짝의 *_id FK 컬럼 채움.
--   7개 entity 에 owner_sfid 만 있고 owner_user_id 짝 컬럼 부재 → resolve 경고 + skip.
--   Application 측에서도 owner User 객체 join 불가.
--   NewProduct 는 추가로 record_type_id 부재 → RecordType FK 채움 skip.
--
-- 적용 (SF describe API dump = 권위 출처):
--   - Account: SF OwnerId.referenceTo = [User] 단일 + 이미 owner_id 가 User FK →
--     owner_id 를 owner_user_id 로 rename + 인덱스 + (XOR 불요)
--   - HolidayMaster / InspectionTheme (Theme__c) / NewProduct / ProductBarcode /
--     PushMessage / UploadFile: SF OwnerId.referenceTo = [Group, User] polymorphic +
--     기존 owner_id 가 Employee FK → V116 패턴 그대로:
--       (a) Employee FK 제약 제거 → (b) owner_id → owner_user_id rename →
--       (c) Employee.id 잔여값 NULL 초기화 (User.user_id 의미 정합 깨짐) →
--       (d) owner_group_id BIGINT 신규 + User/Group FK 제약 + XOR CHECK + 인덱스
--   - NewProduct.record_type_id: RecordTypeId.referenceTo = [RecordType] →
--     record_type.record_type_id FK + 인덱스
--
-- 데이터 처리:
--   - owner_user_id 잔여값은 운영에서 owner_sfid sync buffer 가 보존되어 있어
--     Stage2 fk substep 이 SF describe sfid → user.sfid → user.user_id 로 자동 채움.
--   - Account 만 owner_id 가 User FK 였으므로 rename 만으로 의미 일관 (NULL 초기화 불요).

BEGIN;

-- ============================================================================
-- (1) account — owner_id (User FK) → owner_user_id rename (단일 User, XOR 불요)
-- ============================================================================

-- 기존 User FK 제약 제거 후 컬럼 rename + FK 재추가
ALTER TABLE powersales.account
    DROP CONSTRAINT IF EXISTS fk_account_owner;

ALTER TABLE powersales.account
    RENAME COLUMN owner_id TO owner_user_id;

ALTER TABLE powersales.account
    ADD CONSTRAINT fk_account_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- 기존 owner_id 인덱스 정리 (이름이 idx_account_owner_id 또는 유사) + 새 이름 인덱스
DROP INDEX IF EXISTS powersales.idx_account_owner_id;
CREATE INDEX idx_account_owner_user_id ON powersales.account (owner_user_id);

-- ============================================================================
-- (2) holiday_master — owner_id (Employee FK) → owner_user_id (User FK) + Group polymorphic
-- ============================================================================

ALTER TABLE powersales.holiday_master
    DROP CONSTRAINT IF EXISTS fk_holiday_master_owner;

DROP INDEX IF EXISTS powersales.idx_holiday_master_owner_id;

ALTER TABLE powersales.holiday_master
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.holiday_master SET owner_user_id = NULL;

ALTER TABLE powersales.holiday_master
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.holiday_master
    ADD CONSTRAINT fk_holiday_master_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_holiday_master_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_holiday_master_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_holiday_master_owner_user_id  ON powersales.holiday_master (owner_user_id);
CREATE INDEX idx_holiday_master_owner_group_id ON powersales.holiday_master (owner_group_id);

-- ============================================================================
-- (3) inspection_theme — owner_id (Employee FK) → owner_user_id (User FK) + Group polymorphic
-- ============================================================================

ALTER TABLE powersales.inspection_theme
    DROP CONSTRAINT IF EXISTS fk_inspection_theme_owner;

DROP INDEX IF EXISTS powersales.idx_inspection_theme_owner_id;

ALTER TABLE powersales.inspection_theme
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.inspection_theme SET owner_user_id = NULL;

ALTER TABLE powersales.inspection_theme
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.inspection_theme
    ADD CONSTRAINT fk_inspection_theme_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_inspection_theme_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_inspection_theme_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_inspection_theme_owner_user_id  ON powersales.inspection_theme (owner_user_id);
CREATE INDEX idx_inspection_theme_owner_group_id ON powersales.inspection_theme (owner_group_id);

-- ============================================================================
-- (4) new_product — owner_id (Employee FK) → owner_user_id + Group polymorphic + record_type_id
-- ============================================================================

ALTER TABLE powersales.new_product
    DROP CONSTRAINT IF EXISTS fk_new_product_owner;

DROP INDEX IF EXISTS powersales.idx_new_product_owner_id;

ALTER TABLE powersales.new_product
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.new_product SET owner_user_id = NULL;

ALTER TABLE powersales.new_product
    ADD COLUMN owner_group_id BIGINT,
    ADD COLUMN record_type_id BIGINT;

ALTER TABLE powersales.new_product
    ADD CONSTRAINT fk_new_product_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_new_product_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_new_product_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        ),
    ADD CONSTRAINT fk_new_product_record_type
        FOREIGN KEY (record_type_id) REFERENCES powersales.record_type (record_type_id)
        ON DELETE SET NULL;

CREATE INDEX idx_new_product_owner_user_id  ON powersales.new_product (owner_user_id);
CREATE INDEX idx_new_product_owner_group_id ON powersales.new_product (owner_group_id);
CREATE INDEX idx_new_product_record_type_id ON powersales.new_product (record_type_id);

-- ============================================================================
-- (5) product_barcode — owner_id (Employee FK) → owner_user_id + Group polymorphic
-- ============================================================================

ALTER TABLE powersales.product_barcode
    DROP CONSTRAINT IF EXISTS fk_product_barcode_owner;

DROP INDEX IF EXISTS powersales.idx_product_barcode_owner_id;

ALTER TABLE powersales.product_barcode
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.product_barcode SET owner_user_id = NULL;

ALTER TABLE powersales.product_barcode
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.product_barcode
    ADD CONSTRAINT fk_product_barcode_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_product_barcode_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_product_barcode_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_product_barcode_owner_user_id  ON powersales.product_barcode (owner_user_id);
CREATE INDEX idx_product_barcode_owner_group_id ON powersales.product_barcode (owner_group_id);

-- ============================================================================
-- (6) push_message — owner_id (Employee FK) → owner_user_id + Group polymorphic
-- ============================================================================

ALTER TABLE powersales.push_message
    DROP CONSTRAINT IF EXISTS fk_push_message_owner;

DROP INDEX IF EXISTS powersales.idx_push_message_owner_id;

ALTER TABLE powersales.push_message
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.push_message SET owner_user_id = NULL;

ALTER TABLE powersales.push_message
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.push_message
    ADD CONSTRAINT fk_push_message_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_push_message_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_push_message_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_push_message_owner_user_id  ON powersales.push_message (owner_user_id);
CREATE INDEX idx_push_message_owner_group_id ON powersales.push_message (owner_group_id);

-- ============================================================================
-- (7) upload_file — owner_id (Employee FK) → owner_user_id + Group polymorphic
-- ============================================================================

ALTER TABLE powersales.upload_file
    DROP CONSTRAINT IF EXISTS fk_upload_file_owner;

DROP INDEX IF EXISTS powersales.idx_upload_file_owner_id;

ALTER TABLE powersales.upload_file
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.upload_file SET owner_user_id = NULL;

ALTER TABLE powersales.upload_file
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.upload_file
    ADD CONSTRAINT fk_upload_file_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_upload_file_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_upload_file_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_upload_file_owner_user_id  ON powersales.upload_file (owner_user_id);
CREATE INDEX idx_upload_file_owner_group_id ON powersales.upload_file (owner_group_id);

COMMIT;
