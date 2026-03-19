-- V43: Notice.employeeId sfid(String) → employee_id(BIGINT PK) 전환 (Spec #299)

-- 1. 새 컬럼 추가
ALTER TABLE dkretail__notice__c ADD COLUMN employee_id BIGINT;

-- 2. employee.sfid 기반으로 employee.id 매핑
UPDATE dkretail__notice__c n
SET employee_id = e.id
FROM employee e
WHERE n.employeeid__c = e.sfid;

-- 3. 기존 컬럼 삭제
ALTER TABLE dkretail__notice__c DROP COLUMN employeeid__c;
