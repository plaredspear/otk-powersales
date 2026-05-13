-- Spec #707: AgreementWord SF Object 정합 (Group A + Reference R-2)
ALTER TABLE powersales.agreement_word
    ADD COLUMN owner_sfid            VARCHAR(18),
    ADD COLUMN owner_id              BIGINT REFERENCES powersales.employee(employee_id),
    ADD COLUMN created_by_sfid       VARCHAR(18),
    ADD COLUMN created_by_id         BIGINT REFERENCES powersales.employee(employee_id),
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT REFERENCES powersales.employee(employee_id);

CREATE INDEX idx_agreement_word_owner_id ON powersales.agreement_word(owner_id);
CREATE INDEX idx_agreement_word_created_by_id ON powersales.agreement_word(created_by_id);
CREATE INDEX idx_agreement_word_last_modified_by_id ON powersales.agreement_word(last_modified_by_id);
