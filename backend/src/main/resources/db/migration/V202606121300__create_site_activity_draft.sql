-- 현장점검 등록 임시저장(draft) 테이블.
-- 레거시(otg_PowerSales) salesforce2.tmp_onsite 의 신규 시스템 대응물.
-- 정식 등록(현장점검 생성) 성공 시 해당 사원의 draft row 는 삭제된다.
-- 사원(employee) 1명당 draft 1건(unique). claim_draft 패턴 정합.
-- SF 와 동기화되지 않는 로컬 전용 테이블이므로 SFObject 어노테이션을 두지 않는다.

CREATE TABLE powersales.site_activity_draft (
    site_activity_draft_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id               BIGINT NOT NULL,
    theme_id                  BIGINT,
    category                  VARCHAR(20),
    account_id                BIGINT,
    account_name              VARCHAR(255),
    inspection_date           DATE,
    field_type_code           VARCHAR(30),
    description               VARCHAR(4000),
    product_code              VARCHAR(50),
    product_name              VARCHAR(255),
    competitor_name           VARCHAR(255),
    competitor_activity       VARCHAR(4000),
    competitor_tasting        BOOLEAN,
    competitor_product_name   VARCHAR(255),
    competitor_product_price  INTEGER,
    competitor_sales_quantity INTEGER,
    photo_key_1               VARCHAR(255),
    photo_key_2               VARCHAR(255),
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준 — claim_draft 정합)
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_site_activity_draft_employee UNIQUE (employee_id),
    CONSTRAINT fk_site_activity_draft_employee
        FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id)
        ON DELETE CASCADE
);
