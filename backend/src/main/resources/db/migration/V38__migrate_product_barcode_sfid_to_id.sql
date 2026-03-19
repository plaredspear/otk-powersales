-- V38: ProductBarcode.product_sfid → product_id(PK) 전환 (Spec #294)

-- 1. 새 컬럼 추가
ALTER TABLE product_barcode ADD COLUMN product_id BIGINT;

-- 2. product.sfid 기반으로 product.id 매핑
UPDATE product_barcode pb
SET product_id = p.id
FROM product p
WHERE pb.product_sfid = p.sfid;

-- 3. 기존 컬럼 삭제
ALTER TABLE product_barcode DROP COLUMN product_sfid;
