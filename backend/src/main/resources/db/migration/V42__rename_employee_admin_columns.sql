-- employee_admin 테이블 PK 컬럼명 가독성 개선
-- emp_code → employee_code (Employee 엔티티와 네이밍 통일)
ALTER TABLE salesforce2.employee_admin RENAME COLUMN emp_code TO employee_code;
