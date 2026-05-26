-- spec #680 §6.4 — MFEIS batch 조회 성능 index
--
-- MfeisThisMonthRevenueBatch 의 추출 쿼리
--   SELECT ... FROM monthly_female_employee_integration_schedule
--   WHERE year = ? AND month = ? AND working_category5 LIKE '%상시%'
-- 가 전월 + "상시" 필터로 대상 row 만 빠르게 추출하도록 복합 index 신설.
--
-- working_category5 는 LIKE '%상시%' (양쪽 와일드카드) 라 index seek 는 불가하지만,
-- year + month 두 컬럼이 selectivity 가 높아 index range scan + filter 패턴이
-- full table scan 보다 우월. (운영 row 수 약 수만~수십만, 월별 row 수 수천~수만 수준)

CREATE INDEX IF NOT EXISTS idx_mfeis_year_month_working_category5
    ON powersales.monthly_female_employee_integration_schedule (year, month, working_category5);
