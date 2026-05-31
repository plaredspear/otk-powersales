-- 여사원 일정 무필터 summary covering index 보강 — INCLUDE 에 is_deleted 추가.
--
-- V202606010234 가 (employee_id, working_date) INCLUDE (working_type, working_category1, attendance_log_id)
-- 로 covering index 를 만들었으나, 집계 쿼리(aggregateDailySummaryByEmployeeIds)의 WHERE 에
-- (is_deleted IS NULL OR is_deleted = false) 필터가 있어 is_deleted 가 인덱스에 없으면 해당 판정에
-- 힙 접근이 남는다. is_deleted 를 INCLUDE 에 더해 완전한 Index Only Scan 을 달성한다.
--
-- (V202606010234 는 이미 dev DB 에 적용되어 checksum 이 고정 — 본 컬럼 추가는 신규 V 파일로 분리.)
-- 키/partial 조건은 그대로라 기존 수혜 쿼리 동작 변화 없음.

DROP INDEX IF EXISTS powersales.idx_team_member_schedule_employee_id_working_date;

CREATE INDEX idx_team_member_schedule_employee_id_working_date
    ON powersales.team_member_schedule (employee_id, working_date)
    INCLUDE (working_type, working_category1, attendance_log_id, is_deleted)
    WHERE employee_id IS NOT NULL;
