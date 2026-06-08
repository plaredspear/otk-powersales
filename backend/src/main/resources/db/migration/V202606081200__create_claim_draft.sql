-- 클레임 등록 임시저장(draft) 테이블.
-- 레거시(otg_PowerSales) salesforce2.tmp_claim 의 신규 시스템 대응물.
-- 정식 등록(클레임 생성) 성공 시 해당 사원의 draft row 는 삭제된다.
-- 사원(employee) 1명당 draft 1건(unique). DailySalesDraft 패턴 정합.
-- SF 와 동기화되지 않는 로컬 전용 테이블이므로 SFObject 어노테이션을 두지 않는다.

CREATE TABLE powersales.claim_draft (
    claim_draft_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id           BIGINT NOT NULL,
    account_id            BIGINT,
    account_name          VARCHAR(255),
    product_code          VARCHAR(50),
    product_name          VARCHAR(255),
    date_type             VARCHAR(30),
    claim_date            DATE,
    claim_type1           VARCHAR(10),
    claim_type2           VARCHAR(10),
    defect_description    VARCHAR(4000),
    defect_quantity       NUMERIC,
    purchase_amount       NUMERIC,
    purchase_method_code  VARCHAR(10),
    request_type_code     VARCHAR(255),
    defect_photo_key      VARCHAR(255),
    label_photo_key       VARCHAR(255),
    receipt_photo_key     VARCHAR(255),
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준 — daily_sales_draft 정합)
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_claim_draft_employee UNIQUE (employee_id),
    CONSTRAINT fk_claim_draft_employee
        FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id)
        ON DELETE CASCADE
);
