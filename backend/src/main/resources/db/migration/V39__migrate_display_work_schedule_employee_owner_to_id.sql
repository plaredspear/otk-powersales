-- V39: DisplayWorkSchedule employee_number→employee_id(PK), owner_id→Long(PK), created_by_id 삭제 (Spec #296)

-- Step 1: employeeNumber(사번) → employeeId(PK) 전환
ALTER TABLE display_work_schedule ADD COLUMN employee_id_new BIGINT;

UPDATE display_work_schedule dws
SET employee_id_new = e.id
FROM employee e
WHERE dws.employee_number = e.employee_number
  AND dws.employee_number IS NOT NULL;

ALTER TABLE display_work_schedule DROP COLUMN employee_number;
ALTER TABLE display_work_schedule RENAME COLUMN employee_id_new TO employee_id;

-- Step 2: ownerId → PK 전환 (2단계 매칭: 사번 → sfid)
ALTER TABLE display_work_schedule ADD COLUMN owner_id_new BIGINT;

-- 2a. employee_number로 매칭 (신규 레코드: 사번으로 저장된 값)
UPDATE display_work_schedule dws
SET owner_id_new = e.id
FROM employee e
WHERE dws.owner_id = e.employee_number
  AND dws.owner_id IS NOT NULL;

-- 2b. 미매칭 건에 대해 sfid로 매칭 (레거시 레코드: SF User sfid)
UPDATE display_work_schedule dws
SET owner_id_new = e.id
FROM employee e
WHERE dws.owner_id = e.sfid
  AND dws.owner_id IS NOT NULL
  AND dws.owner_id_new IS NULL;

ALTER TABLE display_work_schedule DROP COLUMN owner_id;
ALTER TABLE display_work_schedule RENAME COLUMN owner_id_new TO owner_id;

-- Step 3: created_by_id 컬럼 삭제
ALTER TABLE display_work_schedule DROP COLUMN created_by_id;
