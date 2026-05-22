-- 스펙 #664 P1-B: Suggestion 도메인 활성화 (Proposal → Suggestion)
-- SF SObject: DKRetail__Proposal__c (사용자 정의 41 필드 + 시스템 audit)
-- Q3 옵션 B (레거시 동등 — R17 WERK bug 재현) / Q6 옵션 1 (DKRetail__Category 폐지) /
-- Q7 옵션 2 (proposal_number 별 컬럼 보존) / Q10 옵션 1 (action_status 본 Part 포함) /
-- Q11 옵션 2 (전체 cut-over) / Q12 채택 (DB Sequence 채번)

-- Sequence — proposal_number 의 NNNNNN 부분 채번 (Q12 채택)
CREATE SEQUENCE powersales.suggestion_proposal_number_seq
    START WITH 100000
    INCREMENT BY 1
    MINVALUE 100000
    MAXVALUE 999999
    CACHE 1;

-- 테이블
CREATE TABLE powersales.suggestion (
    suggestion_id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                            VARCHAR(18) UNIQUE,
    proposal_number                 VARCHAR(80)  NOT NULL,
    title                           VARCHAR(250) NOT NULL,
    content                         TEXT         NOT NULL,
    category                        VARCHAR(50)  NOT NULL,
    category1                       VARCHAR(255),
    category2                       VARCHAR(255),
    category3                       VARCHAR(255),
    sap_account_code                VARCHAR(100),
    employee_sfid                   VARCHAR(18),
    account_sfid                    VARCHAR(18),
    product_sfid                    VARCHAR(18),
    product_code                    VARCHAR(20),
    org_cost_center_code            VARCHAR(100),
    car_number                      VARCHAR(20),
    claim_date                      DATE,
    claim_type                      VARCHAR(200),
    claim_type_measures             VARCHAR(200),
    logistics_responsibility        VARCHAR(20),
    reception_logistics_center      VARCHAR(255),
    responsible_logistics_center    VARCHAR(255),
    status                          VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    action_status                   VARCHAR(30),
    duplicate_proposal_num          VARCHAR(255),
    is_deleted                      BOOLEAN      NOT NULL DEFAULT FALSE,
    account_id                      BIGINT,
    -- Stage1 적재 시점에는 NULL (employee_sfid 만 채워짐), Stage2-A 에서 FK id 변환 후 채워짐.
    -- 신규 INSERT 는 service create() 에서 employee 필수 검증 (Q1 #660 정책).
    employee_id                     BIGINT,
    created_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suggestion_proposal_number UNIQUE (proposal_number),
    CONSTRAINT fk_suggestion_account  FOREIGN KEY (account_id)  REFERENCES powersales.account(account_id),
    CONSTRAINT fk_suggestion_employee FOREIGN KEY (employee_id) REFERENCES powersales.employee(employee_id)
);

COMMENT ON TABLE powersales.suggestion IS '제안사항 (SF DKRetail__Proposal__c 매핑) — Spec #664';
COMMENT ON COLUMN powersales.suggestion.proposal_number IS 'SF Name 자동번호 재현 (Q7 옵션 2). SF migration row 는 SF Name 보존 (proposal-NNNNNN), 신규 INSERT 는 S-YYYYMMDD-NNNNNN (suggestion_proposal_number_seq, Q12)';
COMMENT ON COLUMN powersales.suggestion.category IS 'SF Picklist Category__c (3값): 신제품 제안 / 기존제품 상품가치 향상 / 물류 클레임';
COMMENT ON COLUMN powersales.suggestion.action_status IS 'SF Picklist ActionStatus__c (4값): 미확인 / 조치중 / 조치 완료 / 중복접수 — BR3 검증 의존';
COMMENT ON COLUMN powersales.suggestion.reception_logistics_center IS 'WERK1_TEXT2__c — R17 WERK bug 레거시 동등 재현 (Q3 옵션 B)';
COMMENT ON COLUMN powersales.suggestion.responsible_logistics_center IS 'WERK3_TEXT2__c — R17 WERK bug 레거시 동등 재현 (Q3 옵션 B)';

-- 인덱스
CREATE INDEX idx_suggestion_employee_created ON powersales.suggestion (employee_id, created_at DESC);
CREATE INDEX idx_suggestion_category_created ON powersales.suggestion (category, created_at DESC);
CREATE INDEX idx_suggestion_account_created  ON powersales.suggestion (account_id, created_at DESC);
CREATE INDEX idx_suggestion_is_deleted       ON powersales.suggestion (is_deleted) WHERE is_deleted = FALSE;
