-- Add employee_id column to promotion_employee table
ALTER TABLE promotion_employee ADD COLUMN employee_id VARCHAR(8);

-- Backfill employee_id from employee table using sfid join
UPDATE promotion_employee pe
SET employee_id = e.employee_id
FROM employee e
WHERE pe.employee_sfid IS NOT NULL
  AND pe.employee_sfid = e.sfid;
