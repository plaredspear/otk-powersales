-- Product sf-meta-diff Q8/Q9/Q11/Q13 정합 (V131 후속):
--
-- (Q8) DKRetail__BoxReceivingQuantity__c (SF double precision=18, scale=4):
--      box_receiving_quantity DOUBLE PRECISION → NUMERIC(18, 4). 64-bit float 정밀도 손실 회피.
-- (Q9) DKRetail__StandardUnitPrice__c (SF currency precision=18, scale=2):
--      standard_unit_price DOUBLE PRECISION → NUMERIC(18, 2). 통화 정확도 보존.
-- (Q11) SuperTax__c (SF currency precision=18, scale=0):
--      super_tax DOUBLE PRECISION → NUMERIC(18, 0). 통화 정확도 보존.
-- (Q13) length 좁힘 (SF length 권위 — §6.8 정합 우선):
--      manufacture           VARCHAR(100) → VARCHAR(30)
--      manufacture_detail    VARCHAR(255) → VARCHAR(30)
--      claim_management      VARCHAR(100) → VARCHAR(50)
--
-- Picklist enum 도입 (Q4~Q7) 은 SF picklist 가 placeholder `-` 1개만 정의된 상태라
-- DB 컬럼 타입(VARCHAR(255)) 은 그대로 유지하고 entity 측만 enum + Converter 전환.

-- Q8: box_receiving_quantity NUMERIC(18, 4)
ALTER TABLE powersales.product
    ALTER COLUMN box_receiving_quantity TYPE NUMERIC(18, 4);

-- Q9: standard_unit_price NUMERIC(18, 2)
ALTER TABLE powersales.product
    ALTER COLUMN standard_unit_price TYPE NUMERIC(18, 2);

-- Q11: super_tax NUMERIC(18, 0)
ALTER TABLE powersales.product
    ALTER COLUMN super_tax TYPE NUMERIC(18, 0);

-- Q13: length 좁힘 3건
ALTER TABLE powersales.product
    ALTER COLUMN manufacture        TYPE VARCHAR(30),
    ALTER COLUMN manufacture_detail TYPE VARCHAR(30),
    ALTER COLUMN claim_management   TYPE VARCHAR(50);
