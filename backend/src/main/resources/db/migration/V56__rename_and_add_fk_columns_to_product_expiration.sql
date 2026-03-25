-- ProductExpiration 컬럼명 변경 및 FK 컬럼 추가 (#425)

-- 1. account_id (varchar) → account_name RENAME
ALTER TABLE salesforce2.product_expiration RENAME COLUMN account_id TO account_name;

-- 2. account_id INT 컬럼 신규 추가
ALTER TABLE salesforce2.product_expiration ADD COLUMN account_id INT;

-- 3. product_id (varchar) → product_name RENAME
ALTER TABLE salesforce2.product_expiration RENAME COLUMN product_id TO product_name;

-- 4. product_id BIGINT 컬럼 신규 추가
ALTER TABLE salesforce2.product_expiration ADD COLUMN product_id BIGINT;

-- 5. employee_sfid VARCHAR(18) 컬럼 신규 추가
ALTER TABLE salesforce2.product_expiration ADD COLUMN employee_sfid VARCHAR(18);
