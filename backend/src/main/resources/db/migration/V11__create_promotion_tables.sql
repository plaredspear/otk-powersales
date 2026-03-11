-- 행사마스터 테이블 (V12+V14+V17 통합: 최종 형태로 직접 생성)
CREATE TABLE dkretail__promotion__c (
    id              BIGSERIAL NOT NULL,
    promotion_number VARCHAR(20) NOT NULL,
    promotion_name  VARCHAR(200),
    promotion_type_id BIGINT,
    account_id      INTEGER NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    primary_product_id BIGINT,
    other_product   VARCHAR(200),
    message         VARCHAR(255),
    stand_location  VARCHAR(200),
    target_amount   BIGINT,
    actual_amount   BIGINT DEFAULT 0,
    cost_center_code VARCHAR(100),
    branch_name     VARCHAR(100),
    category        VARCHAR(50),
    product_type    VARCHAR(50),
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    professional_team VARCHAR(100),
    external_id     VARCHAR(50),
    remark          VARCHAR(200),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT promotion_pkey PRIMARY KEY (id),
    CONSTRAINT fk_dk_promotion_type
        FOREIGN KEY (promotion_type_id) REFERENCES dkretail__promotion_type(id)
);

CREATE UNIQUE INDEX idx_dk_promotion_number ON dkretail__promotion__c (promotion_number);
CREATE INDEX idx_dk_promotion_account ON dkretail__promotion__c (account_id);
CREATE INDEX idx_dk_promotion_dates ON dkretail__promotion__c (start_date, end_date);
CREATE INDEX idx_dk_promotion_cost_center ON dkretail__promotion__c (cost_center_code);
CREATE INDEX idx_dk_promotion_type_id ON dkretail__promotion__c (promotion_type_id);
CREATE INDEX idx_dk_promotion_external_id ON dkretail__promotion__c (external_id);

-- 행사 번호 시퀀스 (PM00000001 형식)
CREATE SEQUENCE promotion_number_seq START WITH 1 INCREMENT BY 1;

-- 행사상품 테이블
CREATE TABLE promotion_product (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT NOT NULL REFERENCES dkretail__promotion__c(id),
    product_id      BIGINT NOT NULL,
    is_main_product BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pp_promotion ON promotion_product (promotion_id);
CREATE INDEX idx_pp_product ON promotion_product (product_id);
