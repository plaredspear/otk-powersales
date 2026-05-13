-- Spec #612 — Notice SF 누락 컬럼 1개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: Salesforce Object (`DKRetail__Notice__c`)

ALTER TABLE powersales.notice
    ADD COLUMN title varchar(255);
