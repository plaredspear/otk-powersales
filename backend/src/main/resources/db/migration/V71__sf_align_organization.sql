-- 스펙 #722: Organization SF Object 정합 (Group A + Reference R-2)

ALTER TABLE powersales.organization
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.organization
    ADD CONSTRAINT fk_organization_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_organization_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_organization_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_organization_owner_id ON powersales.organization (owner_id);
CREATE INDEX idx_organization_created_by_id ON powersales.organization (created_by_id);
CREATE INDEX idx_organization_last_modified_by_id ON powersales.organization (last_modified_by_id);
