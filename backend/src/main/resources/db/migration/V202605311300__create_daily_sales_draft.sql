-- P4 일매출등록: 여사원 행사 일매출 마감의 임시저장(draft) 테이블.
-- 레거시(otg_PowerSales) salesforce2.tmp_promotion 의 신규 시스템 대응물.
-- 최종 마감(promotion_employee.promo_close_by_tm=true) 시 해당 draft row 는 삭제된다.
-- promotion_employee 1건당 draft 1건(unique).

CREATE TABLE powersales.daily_sales_draft (
    daily_sales_draft_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    promotion_employee_id     BIGINT NOT NULL,
    employee_id               BIGINT NOT NULL,
    base_price                NUMERIC,
    primary_sales_quantity    NUMERIC,
    primary_sales_price       NUMERIC,
    primary_product_amount    NUMERIC,
    other_sales_quantity      NUMERIC,
    other_sales_amount        NUMERIC,
    description               VARCHAR(50),
    s3_image_unique_key       VARCHAR(255),
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준 — V202605310100__create_site_activity 정합)
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_daily_sales_draft_promotion_employee UNIQUE (promotion_employee_id),
    CONSTRAINT fk_daily_sales_draft_promotion_employee
        FOREIGN KEY (promotion_employee_id) REFERENCES powersales.promotion_employee (promotion_employee_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_daily_sales_draft_employee_id ON powersales.daily_sales_draft (employee_id);
