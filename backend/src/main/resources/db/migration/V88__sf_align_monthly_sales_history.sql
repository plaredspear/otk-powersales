-- 스펙 #729: MonthlySalesHistory SF Object 정합 (Group A + Reference R-2)

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.monthly_sales_history
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.monthly_sales_history
    ADD CONSTRAINT fk_monthly_sales_history_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_sales_history_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_monthly_sales_history_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_monthly_sales_history_owner_id ON powersales.monthly_sales_history (owner_id);
CREATE INDEX idx_monthly_sales_history_created_by_id ON powersales.monthly_sales_history (created_by_id);
CREATE INDEX idx_monthly_sales_history_last_modified_by_id ON powersales.monthly_sales_history (last_modified_by_id);
