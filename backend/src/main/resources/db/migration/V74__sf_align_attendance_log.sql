-- 스펙 #731: AttendanceLog SF Object 정합 (Group A + Reference R-2)

ALTER TABLE powersales.attendance_log
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.attendance_log
    ADD CONSTRAINT fk_attendance_log_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attendance_log_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attendance_log_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_attendance_log_owner_id ON powersales.attendance_log (owner_id);
CREATE INDEX idx_attendance_log_created_by_id ON powersales.attendance_log (created_by_id);
CREATE INDEX idx_attendance_log_last_modified_by_id ON powersales.attendance_log (last_modified_by_id);
