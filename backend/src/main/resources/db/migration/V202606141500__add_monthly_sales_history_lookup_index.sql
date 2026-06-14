-- MonthlySalesHistory 조회 키 복합 인덱스 신규 도입 — 대시보드 매출현황 탭 / 월매출 대시보드 조회 가속.
--
-- 배경:
--   화면/집계 조회는 RDS `MonthlySalesHistory` 적재본을 본다 (외부 ORORA view 직접 호출 아님 —
--   ORORA view 는 RDS 적재 배치에서만 읽음). 조회 경로는 MonthlySalesHistoryQueryGateway 의
--   findBySalesYearInAndSalesMonthInAndSapAccountCodeIn 으로,
--     WHERE sales_year IN (...) AND sales_month IN (...) AND sap_account_code IN (...)
--   형태다. 재생성(V202606012103) 시점 인덱스는 FK 컬럼(account_id / owner_* / *_by_id) 뿐이라
--   위 조회 키 3종에 인덱스가 전무 → full table scan 이었다.
--
-- 조치:
--   조회 selectivity 가 가장 높은 sap_account_code 를 선두로 한 복합 인덱스 추가.
--   동일 키를 쓰는 sumInvestedAccountSales(대시보드 매출현황) / getSummary / getList / buildMonthlyTrend 가속.
--
-- 비고:
--   - account_id 기준 조회 경로(findBy...AccountIdIn)는 기존 idx_monthly_sales_history_account_id 로 커버.
--   - 인덱스 선두 컬럼 sap_account_code 단독 prefix 조회도 본 인덱스로 커버되어 별도 단일 인덱스 불요.

CREATE INDEX idx_monthly_sales_history_sap_account_code_year_month
    ON powersales.monthly_sales_history (sap_account_code, sales_year, sales_month);
