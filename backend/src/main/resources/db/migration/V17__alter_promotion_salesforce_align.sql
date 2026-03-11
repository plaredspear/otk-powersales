-- Spec #192-P1: 행사마스터 Salesforce 정합성 개선
-- 1. promotion_name NOT NULL 해제
-- 2. remark (비고) 컬럼 추가
-- 3. other_product 길이 변경 (500 → 200)
-- 4. message 길이 변경 (1000 → 255)
-- 5. cost_center_code 길이 변경 (10 → 100)

ALTER TABLE salesforce2.dkretail__promotion__c
    ALTER COLUMN promotion_name DROP NOT NULL;

ALTER TABLE salesforce2.dkretail__promotion__c
    ADD COLUMN remark VARCHAR(200);

ALTER TABLE salesforce2.dkretail__promotion__c
    ALTER COLUMN other_product TYPE VARCHAR(200);

ALTER TABLE salesforce2.dkretail__promotion__c
    ALTER COLUMN message TYPE VARCHAR(255);

ALTER TABLE salesforce2.dkretail__promotion__c
    ALTER COLUMN cost_center_code TYPE VARCHAR(100);
