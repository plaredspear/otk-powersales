-- 스펙 #718: OrderRequest SF Object 정합 (Group A + Reference R-2)

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.order_request
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.order_request
    ADD CONSTRAINT fk_order_request_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_order_request_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_order_request_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_order_request_owner_id ON powersales.order_request (owner_id);
CREATE INDEX idx_order_request_created_by_id ON powersales.order_request (created_by_id);
CREATE INDEX idx_order_request_last_modified_by_id ON powersales.order_request (last_modified_by_id);
