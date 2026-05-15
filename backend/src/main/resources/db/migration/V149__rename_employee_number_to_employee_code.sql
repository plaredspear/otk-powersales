-- 사번 필드 명명 통일: employee_number → employee_code
--
-- 도메인 마스터인 Employee.employee_code 와 정합화. Kotlin 필드명은 이미 employeeCode 로
-- 통일되어 있고 (User, StaffReview), 본 마이그레이션으로 DB 컬럼명까지 정합화한다.
--
-- 적용 대상:
--  - powersales."user".employee_number → employee_code (unique not null, length 20)
--  - powersales.staff_review.employee_number → employee_code (nullable, length 1300)
--
-- SF 매핑 (@SFField) 은 그대로 보존 — DKRetail__EmployeeNumber__c (User), EmployeeNumber__c (StaffReview).

ALTER TABLE powersales."user"
    RENAME COLUMN employee_number TO employee_code;

ALTER TABLE powersales.staff_review
    RENAME COLUMN employee_number TO employee_code;
