-- MonthlySalesHistory__c SF 정합 — 누락 합계 필드 신규 도입.
--
-- 단일 권위: Salesforce Object (`MonthlySalesHistory__c`)
-- 조치: SF non-calculated 실저장 합계 필드 2개를 entity / DB 에 신규 도입.
--   - ShipClosingSumAmount__c (물류마감실적_합계, double precision=18 scale=0)
--   - ABCClosingSumAmount__c  (전산마감실적_합계, double precision=18 scale=0)
-- 비고:
--   - ClosingAmountSum__c 는 calculated(formula) 라 적재 제외 — 본 2필드는 SF 가 저장하는 실데이터.
--   - 마이그레이션 대상(common.kts / extract-csv.sh SOQL)에는 이미 등록되어 있었으나
--     entity / DB / Stage1Targets 미정합 상태였음 — 본 마이그레이션으로 정합.
--   - SF scale=0 → DB double precision 매핑 (entity Double? 정합).

ALTER TABLE powersales.monthly_sales_history
    ADD COLUMN ship_closing_sum_amount  double precision,
    ADD COLUMN abc_closing_sum_amount   double precision;
