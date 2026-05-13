-- 스펙 #717: Notice SF Object 정합 (Group A + Reference R-2)

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.notice
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.notice
    ADD CONSTRAINT fk_notice_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_notice_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_notice_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_notice_owner_id ON powersales.notice (owner_id);
CREATE INDEX idx_notice_created_by_id ON powersales.notice (created_by_id);
CREATE INDEX idx_notice_last_modified_by_id ON powersales.notice (last_modified_by_id);
