-- V44: StaffReview.employeeId sfid(String) → employee_id(BIGINT PK) 전환 (Spec #300)

-- 1. 새 컬럼 추가
ALTER TABLE staffreview__c ADD COLUMN employee_id BIGINT;

-- 2. employee.sfid 기반으로 employee.id 매핑
UPDATE staffreview__c sr
SET employee_id = e.id
FROM employee e
WHERE sr.dkretail_employeeid__c = e.sfid;

-- 3. 기존 컬럼 삭제
ALTER TABLE staffreview__c DROP COLUMN dkretail_employeeid__c;
