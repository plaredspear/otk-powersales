-- Spec #611 — ErpOrderProduct SF 누락 컬럼 1개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/ERP주문상품(ERP_OrderProduct__c).md

ALTER TABLE powersales.erp_order_product
    ADD COLUMN box_quantity numeric(18, 0);
