-- 스펙 #725: Promotion SF Object 정합 (Group A + Reference R-2 + 길이 정합)

-- SF 정합 길이 확장 (절단 방지)
-- promotion_name: 200 → 1300 (SF length 1300)
-- stand_location: 200 → 255 (SF picklist length 255)
-- branch_name: 100 → 1300 (SF length 1300)
-- category: 50 → 1300 (SF length 1300)
-- product_type: 50 → 255 (SF picklist length 255)
-- account_code: 100 → 1300 (SF length 1300)
-- product_code: 100 → 1300 (SF length 1300)
ALTER TABLE powersales.promotion
    ALTER COLUMN promotion_name TYPE VARCHAR(1300),
    ALTER COLUMN stand_location TYPE VARCHAR(255),
    ALTER COLUMN branch_name TYPE VARCHAR(1300),
    ALTER COLUMN category TYPE VARCHAR(1300),
    ALTER COLUMN product_type TYPE VARCHAR(255),
    ALTER COLUMN account_code TYPE VARCHAR(1300),
    ALTER COLUMN product_code TYPE VARCHAR(1300);

-- Group A R-2 FK: Owner / CreatedBy / LastModifiedBy (sfid 컬럼은 V1 기존, FK 컬럼 신규)
ALTER TABLE powersales.promotion
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.promotion
    ADD CONSTRAINT fk_promotion_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_promotion_owner_id ON powersales.promotion (owner_id);
CREATE INDEX idx_promotion_created_by_id ON powersales.promotion (created_by_id);
CREATE INDEX idx_promotion_last_modified_by_id ON powersales.promotion (last_modified_by_id);
