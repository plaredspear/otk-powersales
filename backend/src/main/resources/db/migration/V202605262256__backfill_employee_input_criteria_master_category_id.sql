-- spec #680 §5.3 — EmployeeInputCriteriaMaster.category_id 백필
--
-- work2 에 EmployeeInputCriteriaMaster entity ManyToOne 매핑 + employee_input_criteria_master
-- 테이블의 category_id FK 컬럼은 이미 존재 (V136 흡수). 다만 sfid → id 백필이 미수행되어
-- 현재 모든 row 의 category_id 가 NULL 상태 (dev 운영 확인 — 2026-05-26).
--
-- 본 마이그레이션은 category_sfid 가 AccountCategoryMaster.sfid 와 매칭되는 row 의 FK 를 채움.
-- 백필 후 refreshIntegration 의 employeeInputCriteriaMaster lookup (Account.accountType.displayName
-- ↔ AccountCategoryMaster.name → EICM.category_id 매칭) 정상 동작.
--
-- idempotent: category_id IS NULL 조건으로 이미 채워진 row 는 건드리지 않음.

UPDATE powersales.employee_input_criteria_master eicm
SET    category_id = acm.account_category_master_id
FROM   powersales.account_category_master acm
WHERE  acm.sfid = eicm.category_sfid
  AND  eicm.category_id IS NULL
  AND  eicm.category_sfid IS NOT NULL;
