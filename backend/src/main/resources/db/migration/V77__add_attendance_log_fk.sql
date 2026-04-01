ALTER TABLE attendance_log
    ADD CONSTRAINT fk_attendance_log_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id);

ALTER TABLE attendance_log
    ADD CONSTRAINT fk_attendance_log_account
        FOREIGN KEY (account_id) REFERENCES account (account_id);
