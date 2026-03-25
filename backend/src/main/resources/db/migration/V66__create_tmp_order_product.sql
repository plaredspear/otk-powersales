CREATE TABLE tmp_order_product (
    tmp_order_product_id BIGSERIAL    PRIMARY KEY,
    employee_code        VARCHAR(80),
    product_code         VARCHAR(80),
    box_cnt              VARCHAR(80),
    ea_cnt               VARCHAR(80),
    total_cnt            VARCHAR(80),
    employee_id          BIGINT,
    product_id           BIGINT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
