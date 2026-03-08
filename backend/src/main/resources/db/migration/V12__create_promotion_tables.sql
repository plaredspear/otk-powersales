-- 행사마스터 테이블
CREATE TABLE promotion (
    id              BIGSERIAL PRIMARY KEY,
    promotion_number VARCHAR(20) NOT NULL,
    promotion_name  VARCHAR(200) NOT NULL,
    promotion_type  VARCHAR(50),
    account_id      INTEGER NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    primary_product_id BIGINT,
    other_product   VARCHAR(500),
    message         VARCHAR(1000),
    stand_location  VARCHAR(200),
    target_amount   BIGINT,
    cost_center_code VARCHAR(10),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_promotion_number ON promotion (promotion_number);
CREATE INDEX idx_promotion_account ON promotion (account_id);
CREATE INDEX idx_promotion_dates ON promotion (start_date, end_date);
CREATE INDEX idx_promotion_cost_center ON promotion (cost_center_code);

-- 행사 번호 시퀀스 (PM00000001 형식)
CREATE SEQUENCE promotion_number_seq START WITH 1 INCREMENT BY 1;

-- 행사상품 테이블
CREATE TABLE promotion_product (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT NOT NULL REFERENCES promotion(id),
    product_id      BIGINT NOT NULL,
    is_main_product BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pp_promotion ON promotion_product (promotion_id);
CREATE INDEX idx_pp_product ON promotion_product (product_id);
