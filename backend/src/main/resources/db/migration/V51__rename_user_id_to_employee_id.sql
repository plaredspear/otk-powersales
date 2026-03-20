-- 5개 테이블의 user_id FK 컬럼을 employee_id로 RENAME
ALTER TABLE orders RENAME COLUMN user_id TO employee_id;
ALTER TABLE order_drafts RENAME COLUMN user_id TO employee_id;
ALTER TABLE inspections RENAME COLUMN user_id TO employee_id;
ALTER TABLE claims RENAME COLUMN user_id TO employee_id;
ALTER TABLE suggestions RENAME COLUMN user_id TO employee_id;

-- 7개 인덱스 RENAME (PostgreSQL: ALTER INDEX ... RENAME TO ...)
ALTER INDEX idx_orders_user_id RENAME TO idx_orders_employee_id;
ALTER INDEX idx_order_drafts_user_id RENAME TO idx_order_drafts_employee_id;
ALTER INDEX idx_inspection_user_date RENAME TO idx_inspection_employee_date;
ALTER INDEX idx_inspection_user_date_category RENAME TO idx_inspection_employee_date_category;
ALTER INDEX idx_inspections_user_id RENAME TO idx_inspections_employee_id;
ALTER INDEX idx_claim_user_created RENAME TO idx_claim_employee_created;
ALTER INDEX idx_suggestion_user_created RENAME TO idx_suggestion_employee_created;
