-- HolidayMaster post-load: year 컬럼 파생 (holiday_date.year)
-- SF 측에 year 필드가 없고 entity 가 holiday_date 에서 계산. NOT NULL 제약이 있어 Phase 1 직후 채워야 한다.

UPDATE powersales.holiday_master
SET year = EXTRACT(YEAR FROM holiday_date)::INT
WHERE year IS NULL;
