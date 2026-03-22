-- Spec #388: employeeNumber → employeeCode 통일
-- employee 테이블의 employee_number 컬럼을 employee_code로 rename
ALTER TABLE salesforce2.employee RENAME COLUMN employee_number TO employee_code;

-- employee_info 테이블(구 employee_mng)의 employee_number 컬럼(PK)을 employee_code로 rename
ALTER TABLE salesforce2.employee_info RENAME COLUMN employee_number TO employee_code;
