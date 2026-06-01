-- 거래처목표등록마스터 (SF SalesProgressRateMaster__c) 테이블 신규 생성.
--
-- 데이터 권위: SF (HC sync 대상 아님 — Heroku PG 미존재. SF → RDS 단방향 마이그레이션).
-- 스키마 권위: entity `SalesProgressRateMaster` (@Column / @JoinColumn).
-- Formula 필드(6개: AccountCode / AccountName / AccountType / AccoutBranchName / ProgressRate / TargetSum)는
-- SOQL 적재 불가 + 컬럼 미추가 정책에 따라 제외.

CREATE TABLE powersales.sales_progress_rate_master (
    sales_progress_rate_master_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                             VARCHAR(18),
    name                             VARCHAR(80),
    account_cd_upl                   VARCHAR(255),
    business_rate                    DOUBLE PRECISION,
    current_month_sales_amount       DOUBLE PRECISION,
    external_key                     VARCHAR(255),
    fo_target_amount                 DOUBLE PRECISION,
    fr_target_amount                 DOUBLE PRECISION,
    previous_month_sales_amount      DOUBLE PRECISION,
    rm_target_amount                 DOUBLE PRECISION,
    rt_target_amount                 DOUBLE PRECISION,
    target_month                     VARCHAR(100),
    target_sum_amount                DOUBLE PRECISION,
    target_year                      VARCHAR(100),
    account_branch_view              VARCHAR(100),
    account_branch_code              VARCHAR(255),
    is_deleted                       BOOLEAN,
    account_sfid                     VARCHAR(18),
    owner_sfid                       VARCHAR(18),
    created_by_sfid                  VARCHAR(18),
    last_modified_by_sfid            VARCHAR(18),
    -- Relations (FK)
    account_id                       BIGINT,
    owner_user_id                    BIGINT,
    owner_group_id                   BIGINT,
    created_by_id                    BIGINT,
    last_modified_by_id              BIGINT,
    -- BaseEntity
    created_at                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_sales_progress_rate_master_sfid UNIQUE (sfid),
    CONSTRAINT uk_sales_progress_rate_master_external_key UNIQUE (external_key),
    -- OwnerId polymorphic R-2 (referenceTo = [Group, User]) — owner_user / owner_group XOR
    CONSTRAINT chk_sales_progress_rate_master_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        ),
    CONSTRAINT fk_sales_progress_rate_master_account
        FOREIGN KEY (account_id) REFERENCES powersales.account (account_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_sales_progress_rate_master_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_sales_progress_rate_master_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_sales_progress_rate_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_sales_progress_rate_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL
);

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_sprm_account_id            ON powersales.sales_progress_rate_master (account_id);
CREATE INDEX idx_sprm_owner_user_id         ON powersales.sales_progress_rate_master (owner_user_id);
CREATE INDEX idx_sprm_owner_group_id        ON powersales.sales_progress_rate_master (owner_group_id);
CREATE INDEX idx_sprm_created_by_id         ON powersales.sales_progress_rate_master (created_by_id);
CREATE INDEX idx_sprm_last_modified_by_id   ON powersales.sales_progress_rate_master (last_modified_by_id);
