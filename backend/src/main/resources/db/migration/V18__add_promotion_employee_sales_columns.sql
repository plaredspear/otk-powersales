-- Spec #219: 행사사원 목록 Salesforce 저장 필드 6개 추가
ALTER TABLE promotion_employee ADD COLUMN primary_product_amount BIGINT;
ALTER TABLE promotion_employee ADD COLUMN primary_sales_quantity INTEGER;
ALTER TABLE promotion_employee ADD COLUMN primary_sales_price BIGINT;
ALTER TABLE promotion_employee ADD COLUMN other_sales_amount BIGINT;
ALTER TABLE promotion_employee ADD COLUMN other_sales_quantity INTEGER;
ALTER TABLE promotion_employee ADD COLUMN s3_image_unique_key VARCHAR(255);
