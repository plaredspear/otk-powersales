-- 스펙 #730: AttendInfo SF Object 정합 (Group A + Reference R-2 + 길이 절단 방지)

ALTER TABLE powersales.attend_info
    ALTER COLUMN employee_code TYPE VARCHAR(100),
    ALTER COLUMN start_date TYPE VARCHAR(100),
    ALTER COLUMN end_date TYPE VARCHAR(100),
    ALTER COLUMN attend_type TYPE VARCHAR(100),
    ALTER COLUMN status TYPE VARCHAR(100);

ALTER TABLE powersales.attend_info
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.attend_info
    ADD CONSTRAINT fk_attend_info_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attend_info_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attend_info_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_attend_info_owner_id ON powersales.attend_info (owner_id);
CREATE INDEX idx_attend_info_created_by_id ON powersales.attend_info (created_by_id);
CREATE INDEX idx_attend_info_last_modified_by_id ON powersales.attend_info (last_modified_by_id);
