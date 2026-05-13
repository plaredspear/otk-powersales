-- Spec #706: AgreementHistory SF Object 정합 (Group A + Reference R-2)
ALTER TABLE powersales.agreement_history
    ADD COLUMN name                  VARCHAR(80),
    ADD COLUMN owner_sfid            VARCHAR(18),
    ADD COLUMN owner_id              BIGINT REFERENCES powersales.employee(employee_id),
    ADD COLUMN created_by_sfid       VARCHAR(18),
    ADD COLUMN created_by_id         BIGINT REFERENCES powersales.employee(employee_id),
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT REFERENCES powersales.employee(employee_id);

CREATE INDEX idx_agreement_history_owner_id ON powersales.agreement_history(owner_id);
CREATE INDEX idx_agreement_history_created_by_id ON powersales.agreement_history(created_by_id);
CREATE INDEX idx_agreement_history_last_modified_by_id ON powersales.agreement_history(last_modified_by_id);
