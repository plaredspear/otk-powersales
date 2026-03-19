-- V41: PromotionEmployee employee_sfid+employee_number → employee_id(PK) 전환 (Spec #297)

-- Step 1: employeeSfid(sfid) → PK 전환
ALTER TABLE promotion_employee ADD COLUMN employee_id_new BIGINT;

UPDATE promotion_employee pe
SET employee_id_new = e.id
FROM employee e
WHERE pe.employee_sfid = e.sfid
  AND pe.employee_sfid IS NOT NULL;

-- Step 2: 기존 컬럼 정리
ALTER TABLE promotion_employee DROP COLUMN employee_number;
DROP INDEX IF EXISTS idx_pe_employee_sfid;
ALTER TABLE promotion_employee DROP COLUMN employee_sfid;

-- Step 3: 리네이밍
ALTER TABLE promotion_employee RENAME COLUMN employee_id_new TO employee_id;
