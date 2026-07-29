-- ORORA 일매출 조회(기준정보 화면) 인덱스
--
-- AdminDailySalesHistoryService 의 조회 경로는
--   WHERE sap_account_code = ? AND sales_date LIKE 'yyyyMM%'
-- 인데, daily_sales_history 에는 external_key UNIQUE + account_id/owner FK 인덱스만 있어
-- 이 경로가 Seq Scan 이었다 (테이블은 거래처 × 월 누적이라 계속 증가).
--
-- 선두 컬럼을 등치 조건인 sap_account_code 로 두면 후행 sales_date 는 매칭된 소수 행 안에서만
-- 평가되므로, PostgreSQL 이 비-C 로케일에서 LIKE prefix 에 btree 를 쓰지 못하는 제약과 무관하게
-- 조회 비용이 잡힌다.
--
-- account_id FK 인덱스로 대체하지 않는 이유: 조회 키가 SF 이관분 중 FK 미해소 row 까지 포함하도록
-- 원본 거래처코드(sap_account_code) 텍스트이기 때문 (DailySalesHistoryRepository KDoc 참조).
-- 명명 규칙: idx_<table>_<column...> (V8 / V202606222122 동일).
CREATE INDEX idx_daily_sales_history_account_code_sales_date
    ON powersales.daily_sales_history (sap_account_code, sales_date);
