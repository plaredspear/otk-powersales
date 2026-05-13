-- 스펙 #715: AlternativeHoliday SF Object 정합 (Group A + Reference R-2 + 길이 절단 방지)

ALTER TABLE powersales.alternative_holiday
    ALTER COLUMN employee_name TYPE VARCHAR(1300),
    ALTER COLUMN status TYPE VARCHAR(255);

ALTER TABLE powersales.alternative_holiday
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.alternative_holiday
    ADD CONSTRAINT fk_alternative_holiday_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_alternative_holiday_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_alternative_holiday_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_alternative_holiday_owner_id ON powersales.alternative_holiday (owner_id);
CREATE INDEX idx_alternative_holiday_created_by_id ON powersales.alternative_holiday (created_by_id);
CREATE INDEX idx_alternative_holiday_last_modified_by_id ON powersales.alternative_holiday (last_modified_by_id);
