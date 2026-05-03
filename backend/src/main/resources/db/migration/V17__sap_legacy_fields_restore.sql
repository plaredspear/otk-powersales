-- Spec #575: SAP 인바운드 레거시 필드 16개 보존
--
-- account: 13개 컬럼 추가 (모두 nullable, 기존 데이터 영향 없음)
-- product: 2개 컬럼 추가
-- monthly_sales_history: 1개 컬럼 추가

ALTER TABLE powersales.account
    ADD COLUMN account_status_code      VARCHAR(20),
    ADD COLUMN business_type            VARCHAR(50),
    ADD COLUMN business_category        VARCHAR(50),
    ADD COLUMN business_license_number  VARCHAR(50),
    ADD COLUMN email                    VARCHAR(100),
    ADD COLUMN division_name            VARCHAR(50),
    ADD COLUMN sales_dept_name          VARCHAR(50),
    ADD COLUMN consignment_acc          VARCHAR(1),
    ADD COLUMN werk1                    VARCHAR(20),
    ADD COLUMN werk2                    VARCHAR(20),
    ADD COLUMN werk3                    VARCHAR(20),
    ADD COLUMN sales_dept_cost_center   VARCHAR(20),
    ADD COLUMN division_cost_center     VARCHAR(20);

ALTER TABLE powersales.product
    ADD COLUMN product_barcode VARCHAR(50),
    ADD COLUMN pallet          NUMERIC(18, 4);

ALTER TABLE powersales.monthly_sales_history
    ADD COLUMN total_ledger_amount NUMERIC(18, 4);
