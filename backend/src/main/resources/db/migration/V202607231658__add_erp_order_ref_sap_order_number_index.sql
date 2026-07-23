-- ERP 주문 RefSAPOrderNumber 조회 인덱스 추가.
--
-- V202607211100 에서 추가한 ref_sap_order_number 컬럼에 대한 조회 인덱스.
-- SAP 재전송 판별 / 참조 ERP 주문번호 역참조 조회 시 full scan 을 회피한다.
--   - erp_order.ref_sap_order_number
--   - erp_order_product.ref_sap_order_number

CREATE INDEX IF NOT EXISTS idx_erp_order_ref_sap_order_number
    ON powersales.erp_order (ref_sap_order_number);

CREATE INDEX IF NOT EXISTS idx_erp_order_product_ref_sap_order_number
    ON powersales.erp_order_product (ref_sap_order_number);
