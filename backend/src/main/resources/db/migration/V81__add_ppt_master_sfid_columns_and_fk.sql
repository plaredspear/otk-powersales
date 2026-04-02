-- sfid/참조키 컬럼 추가
ALTER TABLE professional_promotion_team_master
    ADD COLUMN account_sfid VARCHAR(18),
    ADD COLUMN employee_number VARCHAR(20);

-- FK 제약 조건 추가
ALTER TABLE professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id),
    ADD CONSTRAINT fk_ppt_master_account
        FOREIGN KEY (account_id) REFERENCES account (account_id);
