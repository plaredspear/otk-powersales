-- Spec #305: Group C 엔티티 타임스탬프 nullable → NOT NULL 전환
-- 대상: system_code_master, daily_sales_history, erp_order, erp_order_product, account

-- 1. NULL 값을 기본값으로 채움
UPDATE system_code_master SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE daily_sales_history SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE erp_order SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE erp_order_product SET updated_at = created_at WHERE updated_at IS NULL;

-- Account: created_at이 NULL인 경우 NOW()로, updated_at은 created_at으로
UPDATE account SET created_at = NOW() WHERE created_at IS NULL;
UPDATE account SET updated_at = created_at WHERE updated_at IS NULL;

-- 2. NOT NULL 제약조건 추가
ALTER TABLE system_code_master ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE daily_sales_history ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE erp_order ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE erp_order_product ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE account ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE account ALTER COLUMN updated_at SET NOT NULL;
