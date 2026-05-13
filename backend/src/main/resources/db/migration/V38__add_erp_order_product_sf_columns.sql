-- Spec #611 — ErpOrderProduct SF 누락 컬럼 1개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: Salesforce Object (`ERP_OrderProduct__c`)

ALTER TABLE powersales.erp_order_product
    ADD COLUMN box_quantity numeric(18, 0);
