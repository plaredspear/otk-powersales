-- 투입현황 대시보드 조회 키 복합/단일 인덱스 신규 도입 — 대시보드 3섹션 조회 가속.
--
-- 배경:
--   투입현황 대시보드(AdminDashboardService) 는 한 요청에서
--     1) MFEIS 를 당월 + 전월(D2 마감) 2회 조회 (year=? AND month=? AND cost_center_code IN (...))
--     2) Employee 를 지점 스코프로 1회 조회 (cost_center_code IN (...))
--   한다. 두 테이블 모두 기존 인덱스는 FK 컬럼(account_id / owner_* / *_by_id) 뿐이라 위 조회 키에
--   인덱스가 전무 → full table scan 이었다 (monthly_sales_history 는 V202606141500 에서 이미 해소).
--
-- 조치:
--   - MFEIS: SF year/month 가 String(VARCHAR) 컬럼이므로 (year, month, cost_center_code) 복합 인덱스.
--     동일 (year=, month=) 선두를 쓰는 환산인원 현황 리포트(findConvertedHeadcountReport) 도 함께 가속.
--   - Employee: cost_center_code 단일 인덱스. 대시보드 기본현황 외에도 여사원 현황/스케줄/진열스케줄 등
--     cost_center_code 기반 조회 경로 다수가 공용으로 가속된다.
--
-- 비고:
--   - account_id 기반 join 은 기존 idx_mfei_schedule_account_id 로 커버 (본 인덱스와 별개 경로).
--   - organization 조회 키(org_cd*, org_nm3) 는 테이블 규모가 작아 seq scan 이 유리할 수 있어 제외.

CREATE INDEX idx_mfeis_year_month_cost_center_code
    ON powersales.monthly_female_employee_integration_schedule (year, month, cost_center_code);

CREATE INDEX idx_employee_cost_center_code
    ON powersales.employee (cost_center_code);
