-- PK 컬럼명을 {table_name}_id 형식으로 통일 (Spec #310)
ALTER TABLE account RENAME COLUMN id TO account_id;
ALTER TABLE organization RENAME COLUMN id TO organization_id;
ALTER TABLE product RENAME COLUMN id TO product_id;
ALTER TABLE appointment RENAME COLUMN id TO appointment_id;
