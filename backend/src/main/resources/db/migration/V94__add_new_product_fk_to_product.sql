-- 스펙 #737: Product ↔ NewProduct FK 후처리 (#723 §4 self-ref 매핑 오류 정정 동반)
--
-- #613/V40 시점에 product.new_product_sfid (VARCHAR 18) 가 sfid 컨벤션으로 추가됨 (FK 보류).
-- 본 마이그레이션에서 new_product_id BIGINT FK 컬럼 + 제약 + 인덱스 추가.
-- cascade: NewProduct 삭제 시 Product 보존 (FK 만 NULL 처리).

ALTER TABLE powersales.product
    ADD COLUMN new_product_id BIGINT;

ALTER TABLE powersales.product
    ADD CONSTRAINT fk_product_new_product
        FOREIGN KEY (new_product_id) REFERENCES powersales.new_product (new_product_id)
        ON DELETE SET NULL;

CREATE INDEX idx_product_new_product_id ON powersales.product (new_product_id);
