-- 스펙 #728: ProfessionalPromotionTeamMaster SF Object 정합 (Group A + Reference R-2 + 길이 정합 + FullName FK)

-- SF 정합 길이 확장 (절단 방지)
-- employee_number: 20 → 1300
-- team_type: 50 → 255 (Picklist length)
-- branch_code: 20 → 255 (CostCenterCode__c length)
-- branch_name: 50 → 1300
ALTER TABLE powersales.professional_promotion_team_master
    ALTER COLUMN employee_number TYPE VARCHAR(1300),
    ALTER COLUMN team_type TYPE VARCHAR(255),
    ALTER COLUMN branch_code TYPE VARCHAR(255),
    ALTER COLUMN branch_name TYPE VARCHAR(1300);

-- FullName__c Reference (Q2 결정: Employee FK)
ALTER TABLE powersales.professional_promotion_team_master
    ADD COLUMN full_name_sfid VARCHAR(18) NULL,
    ADD COLUMN full_name_id BIGINT NULL;

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.professional_promotion_team_master
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_full_name
        FOREIGN KEY (full_name_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_master_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_ppt_master_full_name_id ON powersales.professional_promotion_team_master (full_name_id);
CREATE INDEX idx_ppt_master_owner_id ON powersales.professional_promotion_team_master (owner_id);
CREATE INDEX idx_ppt_master_created_by_id ON powersales.professional_promotion_team_master (created_by_id);
CREATE INDEX idx_ppt_master_last_modified_by_id ON powersales.professional_promotion_team_master (last_modified_by_id);
