-- Spec #615 — PushMessage SF 누락 비수식 4개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: Salesforce Object (`PushMessage__c`)
--
-- 구현 결정:
--   - s_object_record_id: VARCHAR(50) — SF SObjectRecordId__c 텍스트(50) 정합 (스펙 §6.2 추정 18 → SF 정합 50)

ALTER TABLE powersales.push_message
    ADD COLUMN employee_sfid       varchar(18),
    ADD COLUMN branch              varchar(100),
    ADD COLUMN branch_code         varchar(40),
    ADD COLUMN s_object_record_id  varchar(50);
