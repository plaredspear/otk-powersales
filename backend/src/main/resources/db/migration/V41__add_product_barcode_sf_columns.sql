-- Spec #614 — ProductBarcode SF 누락 비수식 1개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/제품바코드(ProductBarcode__c).md
--
-- 구현 결정:
--   - product_code: VARCHAR(255) — SF ProductCode__c 텍스트(255) 정합 (스펙 §6.3 추정 40 → SF 정합 255)

ALTER TABLE powersales.product_barcode
    ADD COLUMN product_code varchar(255);
