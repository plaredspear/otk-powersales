-- SF Number(scale=0) 가정 정정 — 22컬럼 INTEGER/BIGINT → NUMERIC.
--
-- 배경:
--   SF describe 의 scale=0 메타가 강제력 없음 (Apex / Data Loader / API INSERT 로 소수 저장 가능).
--   실제 운영 데이터 확인: Claim.DKRetail__Quantity__c (SF scale=0) 에 0.3 존재 →
--   기존 BIGINT 컬럼으로는 적재 불가. 동일 위험이 다른 21컬럼에도 잠재.
--
--   이전 V137 (ErpOrder.order_sales_amount), V138 (ErpOrderProduct 6컬럼) 의
--   sf-meta-diff 작업이 "SF scale=0 = 정수 도메인" 가정으로 BIGINT 변환했으나,
--   본 마이그레이션이 그 가정을 정정 — NUMERIC 으로 되돌리고 소수 보존.
--
-- 정정 범위 (22컬럼, 7테이블):
--   account, claim, erp_order, erp_order_product,
--   monthly_female_employee_integration_schedule, order_request_product, promotion_employee

-- ============================================================================
-- account
-- ============================================================================
ALTER TABLE powersales.account
    ALTER COLUMN number_of_employees TYPE NUMERIC USING number_of_employees::NUMERIC;

-- ============================================================================
-- claim
-- ============================================================================
ALTER TABLE powersales.claim
    ALTER COLUMN defect_quantity TYPE NUMERIC USING defect_quantity::NUMERIC,
    ALTER COLUMN purchase_amount TYPE NUMERIC USING purchase_amount::NUMERIC;

-- ============================================================================
-- erp_order (V137 정정)
-- ============================================================================
ALTER TABLE powersales.erp_order
    ALTER COLUMN order_sales_amount TYPE NUMERIC USING order_sales_amount::NUMERIC;

-- ============================================================================
-- erp_order_product (V138 정정)
-- ============================================================================
ALTER TABLE powersales.erp_order_product
    ALTER COLUMN order_quantity           TYPE NUMERIC USING order_quantity::NUMERIC,
    ALTER COLUMN shipping_quantity        TYPE NUMERIC USING shipping_quantity::NUMERIC,
    ALTER COLUMN order_sales_line_amount  TYPE NUMERIC USING order_sales_line_amount::NUMERIC,
    ALTER COLUMN shipping_amount          TYPE NUMERIC USING shipping_amount::NUMERIC,
    ALTER COLUMN release_quantity         TYPE NUMERIC USING release_quantity::NUMERIC,
    ALTER COLUMN release_amount           TYPE NUMERIC USING release_amount::NUMERIC;

-- ============================================================================
-- monthly_female_employee_integration_schedule
-- ============================================================================
ALTER TABLE powersales.monthly_female_employee_integration_schedule
    ALTER COLUMN number_of_inputs  TYPE NUMERIC USING number_of_inputs::NUMERIC,
    ALTER COLUMN edi_pos           TYPE NUMERIC USING edi_pos::NUMERIC,
    ALTER COLUMN this_month_amount TYPE NUMERIC USING this_month_amount::NUMERIC;

-- ============================================================================
-- order_request_product
-- ============================================================================
ALTER TABLE powersales.order_request_product
    ALTER COLUMN line_number     TYPE NUMERIC USING line_number::NUMERIC,
    ALTER COLUMN quantity_pieces TYPE NUMERIC USING quantity_pieces::NUMERIC;

-- ============================================================================
-- promotion_employee
-- ============================================================================
ALTER TABLE powersales.promotion_employee
    ALTER COLUMN base_price             TYPE NUMERIC USING base_price::NUMERIC,
    ALTER COLUMN daily_target_count     TYPE NUMERIC USING daily_target_count::NUMERIC,
    ALTER COLUMN primary_product_amount TYPE NUMERIC USING primary_product_amount::NUMERIC,
    ALTER COLUMN primary_sales_quantity TYPE NUMERIC USING primary_sales_quantity::NUMERIC,
    ALTER COLUMN primary_sales_price    TYPE NUMERIC USING primary_sales_price::NUMERIC,
    ALTER COLUMN other_sales_amount     TYPE NUMERIC USING other_sales_amount::NUMERIC,
    ALTER COLUMN other_sales_quantity   TYPE NUMERIC USING other_sales_quantity::NUMERIC;
