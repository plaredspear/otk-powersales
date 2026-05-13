-- 스펙 #734: MonthlyFemaleEmployeeIntegrationSchedule SF Object 정합 (Group A + Reference R-2)

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.monthly_female_employee_integration_schedule
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.monthly_female_employee_integration_schedule
    ADD CONSTRAINT fk_monthly_female_employee_integration_schedule_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_female_employee_integration_schedule_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_female_employee_integration_schedule_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_monthly_female_employee_integration_schedule_owner_id ON powersales.monthly_female_employee_integration_schedule (owner_id);
CREATE INDEX idx_monthly_female_employee_integration_schedule_created_by_id ON powersales.monthly_female_employee_integration_schedule (created_by_id);
CREATE INDEX idx_monthly_female_employee_integration_schedule_last_modified_by_id ON powersales.monthly_female_employee_integration_schedule (last_modified_by_id);
