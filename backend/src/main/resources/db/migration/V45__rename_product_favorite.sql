-- 테이블명 단수형 변환
ALTER TABLE salesforce2.product_favorites RENAME TO product_favorite;

-- PK 컬럼명 snake_case 정규화
ALTER TABLE salesforce2.product_favorite RENAME COLUMN employeecode TO employee_code;
ALTER TABLE salesforce2.product_favorite RENAME COLUMN productcode TO product_code;
