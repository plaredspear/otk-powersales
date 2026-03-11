-- V16: Schedule 테이블에 행사조원 Upsert 외부 키 컬럼 추가 (#191)
ALTER TABLE salesforce2.dkretail__teammemberschedule__c
    ADD COLUMN IF NOT EXISTS dkretail__promotionempidext__c VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tms_promotion_emp_id_ext
    ON salesforce2.dkretail__teammemberschedule__c (dkretail__promotionempidext__c);
