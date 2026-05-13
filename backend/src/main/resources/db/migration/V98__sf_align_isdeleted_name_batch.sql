-- SF Object ↔ Entity 정합 일괄 배치 (sf-object-meta 36개 sobject 분석 기반)
-- README §6 정책 — Group A IsDeleted + SF 표준 Name + 도메인 컬럼 + EmployeeInputCriteriaMaster 신규 생성
-- 그룹: C(IsDeleted 4 컬럼 신규) / D(name+is_deleted 6 entity) / E(도메인 4 entity) / A(EmployeeInputCriteriaMaster 신규)

-- ============================================================
-- C 그룹: IsDeleted 컬럼 신규 4건 (어노테이션만 부여한 7건은 SQL 불요)
-- ============================================================

ALTER TABLE powersales.account_category_master ADD COLUMN is_deleted BOOLEAN;
ALTER TABLE powersales.attendance_log          ADD COLUMN is_deleted BOOLEAN;
ALTER TABLE powersales.erp_order               ADD COLUMN is_deleted BOOLEAN;
ALTER TABLE powersales.holiday_master          ADD COLUMN is_deleted BOOLEAN;

-- ============================================================
-- D 그룹: SF 표준 Name + IsDeleted 추가 (Appointment 의 is_deleted 는 V92 에서 추가됨 — Name 만)
-- ============================================================

ALTER TABLE powersales.appointment                          ADD COLUMN name VARCHAR(80);

ALTER TABLE powersales.attend_info                          ADD COLUMN name VARCHAR(80);
ALTER TABLE powersales.attend_info                          ADD COLUMN is_deleted BOOLEAN;

ALTER TABLE powersales.alternative_holiday                  ADD COLUMN name VARCHAR(80);
ALTER TABLE powersales.alternative_holiday                  ADD COLUMN is_deleted BOOLEAN;

ALTER TABLE powersales.organization                         ADD COLUMN name VARCHAR(80);
ALTER TABLE powersales.organization                         ADD COLUMN is_deleted BOOLEAN;

ALTER TABLE powersales.professional_promotion_team_history  ADD COLUMN name VARCHAR(80);
ALTER TABLE powersales.professional_promotion_team_history  ADD COLUMN is_deleted BOOLEAN;

ALTER TABLE powersales.professional_promotion_team_master   ADD COLUMN name VARCHAR(80);
ALTER TABLE powersales.professional_promotion_team_master   ADD COLUMN is_deleted BOOLEAN;

-- ============================================================
-- E 그룹: 도메인 컬럼 추가 4 entity
-- ============================================================

-- OrderRequest: DKRetail__OrderDate__c (date) + IsDeleted (RequestNumberTest__c 제외 — Label='테스트용')
ALTER TABLE powersales.order_request          ADD COLUMN dk_order_date DATE;
ALTER TABLE powersales.order_request          ADD COLUMN is_deleted    BOOLEAN;

-- OrderRequestProduct: Name + dk_line_number + dk_total_count + total_count + IsDeleted
ALTER TABLE powersales.order_request_product  ADD COLUMN name             VARCHAR(80);
ALTER TABLE powersales.order_request_product  ADD COLUMN dk_line_number   VARCHAR(30);
ALTER TABLE powersales.order_request_product  ADD COLUMN dk_total_count   DOUBLE PRECISION;
ALTER TABLE powersales.order_request_product  ADD COLUMN total_count      DOUBLE PRECISION;
ALTER TABLE powersales.order_request_product  ADD COLUMN is_deleted       BOOLEAN;

-- PromotionEmployee: Name + employee_sfid + dk_work_type2 + IsDeleted
ALTER TABLE powersales.promotion_employee     ADD COLUMN name           VARCHAR(80);
ALTER TABLE powersales.promotion_employee     ADD COLUMN employee_sfid  VARCHAR(18);
ALTER TABLE powersales.promotion_employee     ADD COLUMN dk_work_type2  VARCHAR(255);
ALTER TABLE powersales.promotion_employee     ADD COLUMN is_deleted     BOOLEAN;

-- ErpOrderProduct: Name + erp_order_sfid + IsDeleted + sap_order_number 길이 20→255 (SF 절단 방지)
ALTER TABLE powersales.erp_order_product      ADD COLUMN name             VARCHAR(80);
ALTER TABLE powersales.erp_order_product      ADD COLUMN erp_order_sfid   VARCHAR(18);
ALTER TABLE powersales.erp_order_product      ADD COLUMN is_deleted       BOOLEAN;
ALTER TABLE powersales.erp_order_product      ALTER COLUMN sap_order_number TYPE VARCHAR(255);

-- ============================================================
-- A 그룹: EmployeeInputCriteriaMaster 신규 테이블 (sobject EmployeeInputCriteriaMaster__c)
-- ============================================================

CREATE TABLE powersales.employee_input_criteria_master (
    employee_input_criteria_master_id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                                             VARCHAR(18) UNIQUE,
    name                                             VARCHAR(80),
    -- Custom 도메인 컬럼
    bifurcation_half_person_standard                 NUMERIC(18, 0),
    boundary                                         NUMERIC(18, 0),
    category_sfid                                    VARCHAR(18),
    category_id                                      BIGINT,
    confirm_alert                                    VARCHAR(1300),
    confirmed                                        BOOLEAN NOT NULL DEFAULT FALSE,
    start_date                                       DATE,
    end_date                                         DATE,
    fixed_1_person_standard_amount                   NUMERIC(18, 0),
    type_of_work_1                                   VARCHAR(255),
    account_categorized_code                         VARCHAR(1300),
    bifurcation_half_person_min_amount_in_realm_ran  NUMERIC(18, 0),
    fixed_1_person_min_amount_in_realm_range         NUMERIC(18, 0),
    valid_data                                       VARCHAR(1300),
    valid                                            VARCHAR(1300),
    -- Group A
    is_deleted                                       BOOLEAN,
    owner_sfid                                       VARCHAR(18),
    owner_id                                         BIGINT,
    created_by_sfid                                  VARCHAR(18),
    created_by_id                                    BIGINT,
    last_modified_by_sfid                            VARCHAR(18),
    last_modified_by_id                              BIGINT,
    -- BaseEntity
    created_at                                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_eicm_category
        FOREIGN KEY (category_id) REFERENCES powersales.account_category_master (account_category_master_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_eicm_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_eicm_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_eicm_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_eicm_category_id           ON powersales.employee_input_criteria_master (category_id);
CREATE INDEX idx_eicm_owner_id              ON powersales.employee_input_criteria_master (owner_id);
CREATE INDEX idx_eicm_created_by_id         ON powersales.employee_input_criteria_master (created_by_id);
CREATE INDEX idx_eicm_last_modified_by_id   ON powersales.employee_input_criteria_master (last_modified_by_id);
