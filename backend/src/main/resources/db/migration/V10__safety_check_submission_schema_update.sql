-- SafetyCheckSubmission: employee_id nullable 변경 + master_id(sfid) → display_work_schedule_id(PK) 전환

-- 1. employee_id, working_date를 nullable로 변경
ALTER TABLE salesforce2.safety_check_submission ALTER COLUMN employee_id DROP NOT NULL;
ALTER TABLE salesforce2.safety_check_submission ALTER COLUMN working_date DROP NOT NULL;

-- 2. display_work_schedule_id 컬럼 추가
ALTER TABLE salesforce2.safety_check_submission
    ADD COLUMN display_work_schedule_id BIGINT;

-- 3. unique constraint 변경: (employee_id, working_date) → (employee_id, working_date, display_work_schedule_id)
ALTER TABLE salesforce2.safety_check_submission DROP CONSTRAINT IF EXISTS uq_safety_check_employee_date;
ALTER TABLE salesforce2.safety_check_submission
    ADD CONSTRAINT uq_safety_check_employee_date_schedule
    UNIQUE (employee_id, working_date, display_work_schedule_id);

-- 4. master_id(sfid) → display_work_schedule.id 매핑
UPDATE salesforce2.safety_check_submission s
SET display_work_schedule_id = d.display_work_schedule_id
FROM salesforce2.display_work_schedule d
WHERE s.master_id = d.sfid
  AND s.master_id IS NOT NULL;
