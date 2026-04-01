ALTER TABLE agreement_history
    ADD CONSTRAINT fk_agreement_history_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id);

ALTER TABLE agreement_history
    ADD CONSTRAINT fk_agreement_history_agreement_word
        FOREIGN KEY (agreement_word_id) REFERENCES agreement_word (agreement_word_id);
