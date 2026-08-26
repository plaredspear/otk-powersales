-- 행사사원 목표 대비 실적 보고서 — schedule_date 기간(BETWEEN) 조회가 유일한 필터인데
-- 인덱스가 없어 full scan 으로 동작 (테이블 약 21만 행, 계속 증가). 기간 조회 인덱스 추가.
CREATE INDEX idx_promotion_employee_schedule_date
    ON powersales.promotion_employee (schedule_date);
