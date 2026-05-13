-- 스펙 #721: ErpOrderProduct SF Object 정합 (Group A + Reference R-2)

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.erp_order_product
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.erp_order_product
    ADD CONSTRAINT fk_erp_order_product_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_erp_order_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_erp_order_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_erp_order_product_owner_id ON powersales.erp_order_product (owner_id);
CREATE INDEX idx_erp_order_product_created_by_id ON powersales.erp_order_product (created_by_id);
CREATE INDEX idx_erp_order_product_last_modified_by_id ON powersales.erp_order_product (last_modified_by_id);
