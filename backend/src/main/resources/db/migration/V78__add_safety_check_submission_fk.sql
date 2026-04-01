ALTER TABLE safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id);

ALTER TABLE safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_display_work_schedule
        FOREIGN KEY (display_work_schedule_id) REFERENCES display_work_schedule (display_work_schedule_id);

ALTER TABLE safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_team_member_schedule
        FOREIGN KEY (team_member_schedule_id) REFERENCES team_member_schedule (team_member_schedule_id);
