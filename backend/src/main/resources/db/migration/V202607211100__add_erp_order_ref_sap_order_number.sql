-- ERP 주문 인바운드 RefSAPOrderNumber 수신 컬럼 추가.
--
-- SAP inbound 페이로드에 신규로 전달되는 참조 ERP 주문번호(RefSAPOrderNumber)를 적재한다.
-- SF 레거시(ERP_Order__c / ERP_OrderProduct__c) 메타에는 대응 필드가 없는 신규 컬럼 — SF dual-write 대상 아님.
--   - erp_order.ref_sap_order_number         VARCHAR(80)   (sap_order_number 길이 정합)
--   - erp_order_product.ref_sap_order_number VARCHAR(255)  (sap_order_number 길이 정합)

ALTER TABLE powersales.erp_order
    ADD COLUMN ref_sap_order_number VARCHAR(80);

ALTER TABLE powersales.erp_order_product
    ADD COLUMN ref_sap_order_number VARCHAR(255);
