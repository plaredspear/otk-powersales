-- 제안하기(물류클레임 포함) 등록 임시저장(draft) 테이블.
-- 레거시(otg_PowerSales) salesforce2.tmp_suggest 의 신규 시스템 대응물.
-- 정식 등록(제안 생성) 성공 시 해당 사원의 draft row 는 삭제된다.
-- 사원(employee) 1명당 draft 1건(unique). claim_draft / daily_sales_draft 패턴 정합.
-- SF 와 동기화되지 않는 로컬 전용 테이블이므로 SFObject 어노테이션을 두지 않는다.
-- 사진은 레거시 tmp_suggest 와 동일하게 최대 2장(S3 private key) 보관.

CREATE TABLE powersales.suggestion_draft (
    suggestion_draft_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id              BIGINT NOT NULL,
    category                 VARCHAR(40),
    title                    VARCHAR(255),
    content                  VARCHAR(4000),
    product_code             VARCHAR(50),
    product_name             VARCHAR(255),
    account_id               BIGINT,
    account_name             VARCHAR(255),
    sap_account_code         VARCHAR(100),
    claim_type               VARCHAR(200),
    claim_date               DATE,
    car_number               VARCHAR(20),
    logistics_responsibility VARCHAR(20),
    duplicate_proposal_num   VARCHAR(255),
    action_status            VARCHAR(40),
    photo_key1               VARCHAR(255),
    photo_key2               VARCHAR(255),
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준 — claim_draft 정합)
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_suggestion_draft_employee UNIQUE (employee_id),
    CONSTRAINT fk_suggestion_draft_employee
        FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id)
        ON DELETE CASCADE
);
