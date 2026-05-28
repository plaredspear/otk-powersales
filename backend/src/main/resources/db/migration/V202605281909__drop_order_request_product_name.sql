-- OrderRequestProduct.product_name 제거 — SF DKRetail__OrderRequestProduct__c 에 ProductName 필드 미존재.
-- 신규 application 등록 시 SAP inventory lookup 결과를 cache 했으나, SF 마이그레이션 경로로 들어온 행은
-- 비어 있어 화면 "-" 표시 위험. 조회는 product FK relation (orp.product.name) 으로 전환.

ALTER TABLE order_request_product
    DROP COLUMN IF EXISTS product_name;
