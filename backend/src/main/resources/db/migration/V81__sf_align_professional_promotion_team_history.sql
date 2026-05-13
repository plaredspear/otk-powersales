-- 스펙 #727: ProfessionalPromotionTeamHistory SF Object 정합 (Group A + Reference R-2 + BaseEntity 상속)

-- BaseEntity 상속을 위한 timestamp 컬럼 신규 추가 (Q1 옵션 1)
ALTER TABLE powersales.professional_promotion_team_history
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- SF 정합 길이 확장 (절단 방지) — oldValue__c / newValue__c SF length 255
ALTER TABLE powersales.professional_promotion_team_history
    ALTER COLUMN old_value TYPE VARCHAR(255),
    ALTER COLUMN new_value TYPE VARCHAR(255);

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.professional_promotion_team_history
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.professional_promotion_team_history
    ADD CONSTRAINT fk_professional_promotion_team_history_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_professional_promotion_team_history_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_professional_promotion_team_history_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_professional_promotion_team_history_owner_id ON powersales.professional_promotion_team_history (owner_id);
CREATE INDEX idx_professional_promotion_team_history_created_by_id ON powersales.professional_promotion_team_history (created_by_id);
CREATE INDEX idx_professional_promotion_team_history_last_modified_by_id ON powersales.professional_promotion_team_history (last_modified_by_id);
