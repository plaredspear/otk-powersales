-- =========================================================================
-- V157 — SF 대상 entity 의 application-only NOT NULL 컬럼 nullable 화
-- =========================================================================
-- 배경:
--   V155 (JoinColumn FK), V156 (@Column FK id) 와 같은 맥락. SF target
--   entity 중에는 SF 매핑 어노테이션(@SFField) 이 없는 application-only
--   NOT NULL 컬럼이 있어, Stage 1 raw INSERT 시 컬럼이 INSERT 절에서 빠지고
--   DEFAULT 가 없는 컬럼은 NOT NULL 위반으로 batch abort.
--
--   예: claim.store_name (DEFAULT 없음) — Stage 1 raw INSERT 위반.
--
-- 결정:
--   V155/V156 와 동일 방침. application-only NOT NULL 컬럼 일괄
--   nullable 화. DEFAULT 가 있어도 NOT NULL 제약 자체는 일관성 위해 제거.
--
-- 대상 (14개 컬럼):
--   claim: store_name, date_type, product_code, product_name
--   employee: origin
--   alternative_holiday: created_by_emp_no
--   order_request: total_approved_amount
--   order_request_product: unit_price, pieces_per_box, min_order_unit,
--                          supply_quantity, dc_quantity
--   attendance_log: attendance_type
--   user: is_sales_support, password_change_required
-- =========================================================================

BEGIN;

-- claim
ALTER TABLE powersales.claim                  ALTER COLUMN store_name         DROP NOT NULL;
ALTER TABLE powersales.claim                  ALTER COLUMN date_type          DROP NOT NULL;
ALTER TABLE powersales.claim                  ALTER COLUMN product_code       DROP NOT NULL;
ALTER TABLE powersales.claim                  ALTER COLUMN product_name       DROP NOT NULL;

-- employee
ALTER TABLE powersales.employee               ALTER COLUMN origin             DROP NOT NULL;

-- alternative_holiday
ALTER TABLE powersales.alternative_holiday    ALTER COLUMN created_by_emp_no  DROP NOT NULL;

-- order_request
ALTER TABLE powersales.order_request          ALTER COLUMN total_approved_amount DROP NOT NULL;

-- order_request_product
ALTER TABLE powersales.order_request_product  ALTER COLUMN unit_price         DROP NOT NULL;
ALTER TABLE powersales.order_request_product  ALTER COLUMN pieces_per_box     DROP NOT NULL;
ALTER TABLE powersales.order_request_product  ALTER COLUMN min_order_unit     DROP NOT NULL;
ALTER TABLE powersales.order_request_product  ALTER COLUMN supply_quantity    DROP NOT NULL;
ALTER TABLE powersales.order_request_product  ALTER COLUMN dc_quantity        DROP NOT NULL;

-- attendance_log
ALTER TABLE powersales.attendance_log         ALTER COLUMN attendance_type    DROP NOT NULL;

-- user
ALTER TABLE powersales."user"                 ALTER COLUMN is_sales_support          DROP NOT NULL;
ALTER TABLE powersales."user"                 ALTER COLUMN password_change_required  DROP NOT NULL;

COMMIT;
