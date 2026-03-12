-- V15: employee 테이블/컬럼명 가독성 개선 (Spec #205)

-- 1. 테이블 리네임
ALTER TABLE salesforce2.dkretail__employee__c RENAME TO employee;

-- 2. employee 테이블 컬럼 리네임 (13개)
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__empcode__c TO employee_id;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__birthdate__c TO birth_date;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__status__c TO status;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__apploginactive__c TO app_login_active;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__appauthority__c TO app_authority;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__orgname__c TO org_name;
ALTER TABLE salesforce2.employee RENAME COLUMN costcentercode__c TO cost_center_code;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__workphone__c TO work_phone;
ALTER TABLE salesforce2.employee RENAME COLUMN phone__c TO phone;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__homephone__c TO home_phone;
ALTER TABLE salesforce2.employee RENAME COLUMN dkretail__startdate__c TO start_date;
ALTER TABLE salesforce2.employee RENAME COLUMN agreementflag__c TO agreement_flag;
ALTER TABLE salesforce2.employee RENAME COLUMN createddate TO created_date;

-- 3. employee_mng 테이블 컬럼 리네임 (1개)
ALTER TABLE salesforce2.employee_mng RENAME COLUMN empcode__c TO employee_id;

-- 4. 기존 인덱스 삭제
DROP INDEX IF EXISTS salesforce2.hc_idx_dkretail__employee__c_systemmodstamp;
DROP INDEX IF EXISTS salesforce2.hcu_idx_dkretail__employee__c_sfid;

-- 5. 새 인덱스 생성
CREATE INDEX idx_employee_system_mod_stamp ON salesforce2.employee (systemmodstamp);
CREATE UNIQUE INDEX uq_employee_sfid ON salesforce2.employee (sfid);

-- 6. employee_id UNIQUE 인덱스 추가
CREATE UNIQUE INDEX uq_employee_employee_id ON salesforce2.employee (employee_id);

-- 7. PK 제약조건 리네임
ALTER TABLE salesforce2.employee RENAME CONSTRAINT dkretail__employee__c_pkey TO employee_pkey;

-- 8. 시퀀스 리네임
ALTER SEQUENCE salesforce2.dkretail__employee__c_id_seq RENAME TO employee_id_seq;

-- 9. FK 제약조건 리네임 (agreementhistory__c → employee)
ALTER TABLE salesforce2.agreementhistory__c RENAME CONSTRAINT agreementhistory__c_employeeid__c_fkey TO agreementhistory__c_employee_id_fkey;
