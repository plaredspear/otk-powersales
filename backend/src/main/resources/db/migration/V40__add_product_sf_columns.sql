-- Spec #613 — Product SF 누락 비수식 6개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/제품(DKRetail__Product__c).md
--
-- 구현 결정:
--   - barcode: VARCHAR(100) — SF DKRetail__Barcode__c 텍스트(100) 정합 (스펙 §6.3 추정 40 → SF 정합 100)
--   - new_product_sfid: VARCHAR(18) — SF New_Product__c 가 lookup 필드 → *_sfid 컨벤션 정합

ALTER TABLE powersales.product
    ADD COLUMN barcode              varchar(100),
    ADD COLUMN manufacture          varchar(100),
    ADD COLUMN manufacture_detail   varchar(255),
    ADD COLUMN claim_management     varchar(100),
    ADD COLUMN new_product_sfid     varchar(18),
    ADD COLUMN store_condition_text varchar(255);
