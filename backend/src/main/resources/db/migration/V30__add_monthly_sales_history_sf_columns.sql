-- Spec #601 — MonthlySalesHistory SF 누락 컬럼 17개 신규 도입.
--
-- 단일 권위: Salesforce Object (`MonthlySalesHistory__c`)
-- DB 컬럼명 정책:
--   - 시리즈성 컬럼은 SF API Name 패턴 직역 유지 (Q1 결정 — 기존 abc_closing_amount1~3 패턴 일관성)
--   - 비시리즈는 한국어 라벨 의미 + snake_case 가독성
--   - Lookup 필드는 <관계명>_sfid (Q3 결정)
--   - Boolean 은 is_* prefix (JPA 컨벤션)

ALTER TABLE powersales.monthly_sales_history
    ADD COLUMN account_sfid                     varchar(18),
    ADD COLUMN sap_account_code                 varchar(100),
    ADD COLUMN sales_date                       date,
    ADD COLUMN last_monthly_sales_history_sfid  varchar(18),
    ADD COLUMN is_confirmed                     boolean,
    ADD COLUMN hq_review_sfid                   varchar(18),
    ADD COLUMN remark                           text,
    ADD COLUMN ship_closing_amount_nh           numeric(18, 0),
    ADD COLUMN ship_closing_amount1             numeric(18, 0),
    ADD COLUMN ship_closing_amount2             numeric(18, 0),
    ADD COLUMN ship_closing_amount3             numeric(18, 0),
    ADD COLUMN ship_closing_amount4             numeric(18, 0),
    ADD COLUMN ship_closing_sum_amount          numeric(18, 0),
    ADD COLUMN abc_closing_amount4              numeric(18, 0),
    ADD COLUMN abc_closing_sum_amount           numeric(18, 0),
    ADD COLUMN last_month_target_by_hand        numeric(18, 0),
    ADD COLUMN this_month_target                numeric(18, 0);
