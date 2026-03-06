-- Spec #145: SAP 주문/클레임 동기화

-- 1. erp_order 테이블 생성
CREATE TABLE erp_order (
    id               BIGSERIAL PRIMARY KEY,
    sap_order_number VARCHAR(20) NOT NULL,
    sap_account_code VARCHAR(20),
    sap_account_name VARCHAR(100),
    delivery_request_date VARCHAR(8),
    order_date       VARCHAR(8),
    employee_code    VARCHAR(20),
    employee_name    VARCHAR(50),
    order_sales_amount DOUBLE PRECISION,
    order_channel    VARCHAR(10),
    order_channel_nm VARCHAR(50),
    order_type       VARCHAR(10),
    order_type_nm    VARCHAR(50),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP
);

CREATE UNIQUE INDEX uq_erp_order_number ON erp_order (sap_order_number);
CREATE INDEX idx_erp_order_account ON erp_order (sap_account_code);
CREATE INDEX idx_erp_order_date ON erp_order (order_date);

-- 2. erp_order_product 테이블 생성
CREATE TABLE erp_order_product (
    id                      BIGSERIAL PRIMARY KEY,
    erp_order_id            BIGINT NOT NULL REFERENCES erp_order(id),
    sap_order_number        VARCHAR(20) NOT NULL,
    line_number             VARCHAR(10) NOT NULL,
    external_key            VARCHAR(50) NOT NULL,
    product_code            VARCHAR(20),
    product_name            VARCHAR(100),
    order_quantity          DOUBLE PRECISION,
    unit                    VARCHAR(10),
    confirm_quantity_box    DOUBLE PRECISION,
    confirm_quantity        DOUBLE PRECISION,
    confirm_unit            VARCHAR(10),
    default_reason          VARCHAR(100),
    line_item_status        VARCHAR(20),
    delivery_status         VARCHAR(10),
    shipping_driver_name    VARCHAR(50),
    shipping_vehicle        VARCHAR(20),
    shipping_driver_phone   VARCHAR(20),
    shipping_schedule_time  VARCHAR(20),
    shipping_complete_time  VARCHAR(20),
    shipping_quantity_box   DOUBLE PRECISION,
    shipping_quantity       DOUBLE PRECISION,
    order_sales_line_amount DOUBLE PRECISION,
    shipping_amount         DOUBLE PRECISION,
    plant                   VARCHAR(10),
    plant_nm                VARCHAR(50),
    release_quantity        DOUBLE PRECISION,
    release_amount          DOUBLE PRECISION,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
);

CREATE UNIQUE INDEX uq_erp_order_product_external_key ON erp_order_product (external_key);
CREATE INDEX idx_erp_order_product_order_id ON erp_order_product (erp_order_id);

-- 3. tmp_claim 테이블에 id PK 및 SAP 연동 컬럼 추가
ALTER TABLE tmp_claim ADD COLUMN id BIGSERIAL;
ALTER TABLE tmp_claim ADD PRIMARY KEY (id);
ALTER TABLE tmp_claim ADD COLUMN claim_name     VARCHAR(80);
ALTER TABLE tmp_claim ADD COLUMN claim_sequence  VARCHAR(80);
ALTER TABLE tmp_claim ADD COLUMN action_code     VARCHAR(20);
ALTER TABLE tmp_claim ADD COLUMN claim_status    VARCHAR(40);
ALTER TABLE tmp_claim ADD COLUMN claim_content   TEXT;
ALTER TABLE tmp_claim ADD COLUMN reason_type     VARCHAR(80);
ALTER TABLE tmp_claim ADD COLUMN cosmos_key      VARCHAR(80);
