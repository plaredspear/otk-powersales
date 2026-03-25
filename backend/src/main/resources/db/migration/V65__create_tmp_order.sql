CREATE TABLE tmp_order (
    tmp_order_id   BIGSERIAL    PRIMARY KEY,
    employee_code  VARCHAR(80),
    account_code   VARCHAR(80),
    order_date     DATE,
    total_amount   VARCHAR(80),
    account_id     BIGINT,
    employee_id    BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
