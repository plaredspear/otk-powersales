-- V34: SAP 엔티티 Unique Constraint 추가 (Spec #301)
-- 5개 테이블의 External ID / upsert 키 필드에 UNIQUE 제약 추가
-- 1단계: 중복 데이터 제거 (최대 id 보존)
-- 2단계: UNIQUE 제약 추가

-- ============================================================
-- 1. account.external_key
-- ============================================================
DELETE FROM account
WHERE id NOT IN (
    SELECT MAX(id) FROM account
    WHERE external_key IS NOT NULL
    GROUP BY external_key
)
AND external_key IS NOT NULL
AND EXISTS (
    SELECT 1 FROM account a2
    WHERE a2.external_key = account.external_key
    AND a2.id > account.id
);

ALTER TABLE account
    ADD CONSTRAINT uk_account_external_key UNIQUE (external_key);

-- ============================================================
-- 2. product.product_code
-- ============================================================
DELETE FROM product
WHERE id NOT IN (
    SELECT MAX(id) FROM product
    WHERE product_code IS NOT NULL
    GROUP BY product_code
)
AND product_code IS NOT NULL
AND EXISTS (
    SELECT 1 FROM product p2
    WHERE p2.product_code = product.product_code
    AND p2.id > product.id
);

ALTER TABLE product
    ADD CONSTRAINT uk_product_product_code UNIQUE (product_code);

-- ============================================================
-- 3. erp_order.sap_order_number
-- ============================================================
DELETE FROM erp_order
WHERE id NOT IN (
    SELECT MAX(id) FROM erp_order
    GROUP BY sap_order_number
)
AND EXISTS (
    SELECT 1 FROM erp_order e2
    WHERE e2.sap_order_number = erp_order.sap_order_number
    AND e2.id > erp_order.id
);

ALTER TABLE erp_order
    ADD CONSTRAINT uk_erp_order_sap_order_number UNIQUE (sap_order_number);

-- ============================================================
-- 4. monthlysaleshistory__c.externalkey__c
-- ============================================================
DELETE FROM monthlysaleshistory__c
WHERE id NOT IN (
    SELECT MAX(id) FROM monthlysaleshistory__c
    WHERE externalkey__c IS NOT NULL
    GROUP BY externalkey__c
)
AND externalkey__c IS NOT NULL
AND EXISTS (
    SELECT 1 FROM monthlysaleshistory__c m2
    WHERE m2.externalkey__c = monthlysaleshistory__c.externalkey__c
    AND m2.id > monthlysaleshistory__c.id
);

ALTER TABLE monthlysaleshistory__c
    ADD CONSTRAINT uk_monthlysaleshistory_externalkey UNIQUE (externalkey__c);

-- ============================================================
-- 5. product_barcode.custom_key
-- ============================================================
DELETE FROM product_barcode
WHERE id NOT IN (
    SELECT MAX(id) FROM product_barcode
    WHERE custom_key IS NOT NULL
    GROUP BY custom_key
)
AND custom_key IS NOT NULL
AND EXISTS (
    SELECT 1 FROM product_barcode pb2
    WHERE pb2.custom_key = product_barcode.custom_key
    AND pb2.id > product_barcode.id
);

ALTER TABLE product_barcode
    ADD CONSTRAINT uk_product_barcode_custom_key UNIQUE (custom_key);
