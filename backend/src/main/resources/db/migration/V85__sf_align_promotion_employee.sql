-- 스펙 #726: PromotionEmployee SF Object 정합 (Group A + Reference R-2 + 길이 정합)

-- SF 정합 길이 확장 (절단 방지)
-- work_status: 20 → 255 (SF picklist length 255)
-- work_type1: 100 → 255 (SF picklist length 255)
-- work_type3: 100 → 255 (SF picklist length 255)
-- work_type4: 100 → 1300 (SF length 1300)
-- professional_promotion_team: 100 → 1300 (SF length 1300)
ALTER TABLE powersales.promotion_employee
    ALTER COLUMN work_status TYPE VARCHAR(255),
    ALTER COLUMN work_type1 TYPE VARCHAR(255),
    ALTER COLUMN work_type3 TYPE VARCHAR(255),
    ALTER COLUMN work_type4 TYPE VARCHAR(1300),
    ALTER COLUMN professional_promotion_team TYPE VARCHAR(1300);

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.promotion_employee
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.promotion_employee
    ADD CONSTRAINT fk_promotion_employee_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_employee_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_employee_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_promotion_employee_owner_id ON powersales.promotion_employee (owner_id);
CREATE INDEX idx_promotion_employee_created_by_id ON powersales.promotion_employee (created_by_id);
CREATE INDEX idx_promotion_employee_last_modified_by_id ON powersales.promotion_employee (last_modified_by_id);
