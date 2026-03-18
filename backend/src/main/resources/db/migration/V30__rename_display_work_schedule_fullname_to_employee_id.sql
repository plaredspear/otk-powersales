-- Spec #292: DisplayWorkSchedule fullName → employeeId 리네이밍
-- 컬럼명 변경: full_name → employee_id
-- 컬럼 길이 변경: varchar(18) → varchar(100) (TeamMemberSchedule.employeeId와 동일)
ALTER TABLE display_work_schedule RENAME COLUMN full_name TO employee_id;
ALTER TABLE display_work_schedule ALTER COLUMN employee_id TYPE varchar(100);
