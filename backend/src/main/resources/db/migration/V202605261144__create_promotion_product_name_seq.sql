-- DKRetail__PromotionProduct__c.Name (AutoNumber `PS{00000000}`) 8자리 sequence.
-- 레거시 SF 의 Name AutoNumber 동등.
CREATE SEQUENCE IF NOT EXISTS promotion_product_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;
