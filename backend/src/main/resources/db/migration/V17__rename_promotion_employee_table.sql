-- V17: Rename promotion_employee table from legacy Salesforce naming
-- Spec #221: PromotionEmployee 테이블명 가독성 개선

-- 1. Rename table
ALTER TABLE salesforce2.dkretail__promotion_employee__c
    RENAME TO promotion_employee;

-- 2. Rename PK constraint
ALTER TABLE salesforce2.promotion_employee
    RENAME CONSTRAINT dkretail__promotion_employee__c_pkey
    TO promotion_employee_pkey;

-- 3. Rename sequence
ALTER SEQUENCE salesforce2.dkretail__promotion_employee__c_id_seq
    RENAME TO promotion_employee_id_seq;
