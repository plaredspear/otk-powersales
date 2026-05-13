-- 스펙 #724: ProductBarcode SF Object 정합 (Group A + Reference R-2)

ALTER TABLE powersales.product_barcode
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.product_barcode
    ADD CONSTRAINT fk_product_barcode_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_product_barcode_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_product_barcode_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_product_barcode_owner_id ON powersales.product_barcode (owner_id);
CREATE INDEX idx_product_barcode_created_by_id ON powersales.product_barcode (created_by_id);
CREATE INDEX idx_product_barcode_last_modified_by_id ON powersales.product_barcode (last_modified_by_id);
