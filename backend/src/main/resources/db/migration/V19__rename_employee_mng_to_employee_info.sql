-- employee_mng → employee_info 테이블 리네이밍
ALTER TABLE salesforce2.employee_mng RENAME TO employee_info;

-- PK 제약조건 리네이밍
ALTER TABLE salesforce2.employee_info RENAME CONSTRAINT employee_mng_pkey TO employee_info_pkey;
