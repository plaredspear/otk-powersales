ALTER TABLE professional_promotion_team_history
    ADD CONSTRAINT fk_ppt_history_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id);
