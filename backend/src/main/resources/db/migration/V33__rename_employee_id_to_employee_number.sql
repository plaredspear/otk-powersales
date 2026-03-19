-- Rename employee_id column to employee_number across 8 tables
ALTER TABLE employee RENAME COLUMN employee_id TO employee_number;
ALTER TABLE employee_mng RENAME COLUMN employee_id TO employee_number;
ALTER TABLE team_member_schedule RENAME COLUMN employee_id TO employee_number;
ALTER TABLE display_work_schedule RENAME COLUMN employee_id TO employee_number;
ALTER TABLE promotion_employee RENAME COLUMN employee_id TO employee_number;
ALTER TABLE alternative_holiday RENAME COLUMN employee_id TO employee_number;
ALTER TABLE safety_check_submission RENAME COLUMN employee_id TO employee_number;
ALTER TABLE expirationdate__mng RENAME COLUMN employee_id TO employee_number;
