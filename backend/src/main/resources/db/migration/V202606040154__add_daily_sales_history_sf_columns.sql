-- DailySalesHistory__c SF 정합 — 마이그레이션 대상 전환.
--
-- 단일 권위: Salesforce Object (`DailySalesHistory__c`)
-- 조치:
--   1. SF 누락 컬럼 신규 도입 (sfid / account_sfid / erp_sales_amount / erp_distribution_amount)
--   2. account_id FK 컬럼 (Stage 2 fk substep 이 account_sfid → account_id 채움)
--   3. 기존 컬럼 길이 SF 정합 확대 (절단 방지)
-- DB 컬럼명 정책:
--   - 시리즈성 컬럼은 SF API Name 패턴 직역 유지 (erp_sales_amount1~3 일관성)
--   - 합계 필드는 시리즈 prefix 그대로 (erp_sales_amount / erp_distribution_amount)
--   - Lookup 필드는 <관계명>_sfid

ALTER TABLE powersales.daily_sales_history
    ADD COLUMN sfid                     varchar(18),
    ADD COLUMN account_sfid             varchar(18),
    ADD COLUMN account_id               integer,
    ADD COLUMN erp_sales_amount         double precision,
    ADD COLUMN erp_distribution_amount  double precision;

-- SF 정합 — 절단 방지 길이 확대 (sap_account_code 20→100, external_key 30→40)
ALTER TABLE powersales.daily_sales_history
    ALTER COLUMN sap_account_code TYPE varchar(100),
    ALTER COLUMN external_key     TYPE varchar(40);

-- account FK (MonthlySalesHistory 선례와 동일 — 단순 lookup, ON DELETE 미지정)
ALTER TABLE powersales.daily_sales_history
    ADD CONSTRAINT daily_sales_history_account_id_fkey
        FOREIGN KEY (account_id) REFERENCES powersales.account (account_id);
