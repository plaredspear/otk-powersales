-- Spec #623: OrderRequestProduct SF 누락 컬럼 5개 도입 (DKRetail__OrderRequestProduct__c)
ALTER TABLE order_request_product
    ADD COLUMN status        VARCHAR(255),
    ADD COLUMN product_sfid  VARCHAR(18),
    ADD COLUMN box           NUMERIC(18, 0),
    ADD COLUMN piece         NUMERIC(18, 0),
    ADD COLUMN box_quantity  NUMERIC(15, 3);
