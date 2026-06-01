-- MonthlySalesHistory 테이블 재생성.
--
-- 폐기 경위: V202605292156__drop_monthly_sales_history 에서 ORORA view (OroraMonthlySalesHistory)
-- 일원화로 DROP 되었으나, ORORA 직접 조회 성능 한계로 SF `MonthlySalesHistory__c` 를 RDS 로 복제 적재해
-- 자체 관리하기로 결정 → entity / Repository / SF migration 메타 (Stage1Targets) 복원과 함께 테이블 재생성.
--
-- 스키마 권위: entity `MonthlySalesHistory` (@Column / @JoinColumn) — 폐기 직전 최종형
-- (V145 owner polymorphic R-2 + audit User FK + picklist enum + currency NUMERIC(18,0),
--  V165 timestamptz) 를 반영. PK 는 신규 테이블 표준 GENERATED ALWAYS AS IDENTITY.

CREATE TABLE powersales.monthly_sales_history (
    monthly_sales_history_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                             VARCHAR(18),
    name                             VARCHAR(80),
    sales_year                       VARCHAR(4),
    sales_month                      VARCHAR(2),
    last_month_results               NUMERIC(18, 0),
    ship_closing_amount              DOUBLE PRECISION,
    abc_closing_amount1              DOUBLE PRECISION,
    abc_closing_amount2              DOUBLE PRECISION,
    abc_closing_amount3              DOUBLE PRECISION,
    ambient_purpose                  DOUBLE PRECISION,
    fridge_purpose                   DOUBLE PRECISION,
    is_deleted                       BOOLEAN,
    external_key                     VARCHAR(40),
    rl_sales                         DOUBLE PRECISION,
    total_ledger_amount              NUMERIC(18, 0),
    account_sfid                     VARCHAR(18),
    sap_account_code                 VARCHAR(100),
    sales_date                       DATE,
    last_monthly_sales_history_sfid  VARCHAR(18),
    is_confirmed                     BOOLEAN,
    remark                           TEXT,
    ship_closing_amount_nh           DOUBLE PRECISION,
    ship_closing_amount1             DOUBLE PRECISION,
    ship_closing_amount2             DOUBLE PRECISION,
    ship_closing_amount3             DOUBLE PRECISION,
    ship_closing_amount4             DOUBLE PRECISION,
    ship_closing_sum_amount          DOUBLE PRECISION,
    abc_closing_amount4              DOUBLE PRECISION,
    abc_closing_sum_amount           DOUBLE PRECISION,
    last_month_target_by_hand        NUMERIC(18, 0),
    this_month_target                NUMERIC(18, 0),
    owner_sfid                       VARCHAR(18),
    created_by_sfid                  VARCHAR(18),
    last_modified_by_sfid            VARCHAR(18),
    -- Relations (FK)
    account_id                       BIGINT,
    owner_user_id                    BIGINT,
    owner_group_id                   BIGINT,
    created_by_id                    BIGINT,
    last_modified_by_id              BIGINT,
    last_monthly_sales_history_id    BIGINT,
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준)
    created_at                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_monthly_sales_history_sfid UNIQUE (sfid),
    CONSTRAINT uk_monthly_sales_history_external_key UNIQUE (external_key),
    -- OwnerId polymorphic R-2 (referenceTo = [Group, User]) — owner_user / owner_group XOR
    CONSTRAINT chk_monthly_sales_history_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        ),
    CONSTRAINT fk_monthly_sales_history_account
        FOREIGN KEY (account_id) REFERENCES powersales.account (account_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_monthly_sales_history_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_monthly_sales_history_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_monthly_sales_history_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_monthly_sales_history_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_monthly_sales_history_last_monthly_sales_history
        FOREIGN KEY (last_monthly_sales_history_id) REFERENCES powersales.monthly_sales_history (monthly_sales_history_id)
        ON DELETE SET NULL
);

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_monthly_sales_history_account_id            ON powersales.monthly_sales_history (account_id);
CREATE INDEX idx_monthly_sales_history_owner_user_id         ON powersales.monthly_sales_history (owner_user_id);
CREATE INDEX idx_monthly_sales_history_owner_group_id        ON powersales.monthly_sales_history (owner_group_id);
CREATE INDEX idx_monthly_sales_history_created_by_id         ON powersales.monthly_sales_history (created_by_id);
CREATE INDEX idx_monthly_sales_history_last_modified_by_id   ON powersales.monthly_sales_history (last_modified_by_id);
CREATE INDEX idx_monthly_sales_history_last_msh_id           ON powersales.monthly_sales_history (last_monthly_sales_history_id);
