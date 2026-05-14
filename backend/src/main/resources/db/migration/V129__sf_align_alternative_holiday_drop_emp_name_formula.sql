-- SF 메타 정합 (DKRetail__AlternativeHoliday__c §6.7 Formula 컬럼 제거)
-- DKRetail__EmpName__c 는 SF Formula 필드 (DKRetail__EmployeeId__r.Name).
-- entity 의 `employee: Employee?` relation 으로 `employee.name` 조회 대체.
ALTER TABLE alternative_holiday DROP COLUMN IF EXISTS emp_name;
