-- 여사원 일정관리 무필터 summary 집계 쿼리 가속 — covering index 전환.
--
-- 대상 쿼리: TeamMemberScheduleRepositoryCustomImpl.aggregateDailySummaryByEmployeeIds
--   WHERE employee_id IN (...) AND working_date BETWEEN ? AND ? AND (is_deleted IS NULL OR is_deleted = false)
--   GROUP BY working_date  (SELECT 집계: working_type / working_category1 / attendance_log_id 의 조건부 COUNT)
--
-- 기존 idx_team_member_schedule_employee_id_working_date (employee_id, working_date) 가 leading eq + range
-- 탐색은 이미 가속하지만, 집계가 working_type / working_category1 / attendance_log_id 를 읽어야 해서
-- 매칭 row 마다 힙 접근(table lookup)이 발생한다. 데이터가 많은 날(예: 진열 27/27)일수록 힙 접근 비용 누적.
--
-- 본 마이그레이션은 기존 인덱스를 DROP 하고 동일 (employee_id, working_date) 키에 INCLUDE 3컬럼을 더한
-- covering index 로 재생성한다 → 집계가 인덱스만으로 충족되어 Index Only Scan 가능.
-- leading key (employee_id, working_date) 는 그대로라 기존 수혜 8개 쿼리(findMonthlyByEmployeeIds 등)도
-- 동일하게 인덱스를 탄다. partial 조건(employee_id IS NOT NULL)도 기존과 동일 유지 — 다른 쿼리 호환.
--
-- INCLUDE 컬럼은 정렬 키가 아니라 leaf 페이로드라 인덱스 정렬/선택도 영향 없이 페이지 폭만 소폭 증가.

DROP INDEX IF EXISTS powersales.idx_team_member_schedule_employee_id_working_date;

CREATE INDEX idx_team_member_schedule_employee_id_working_date
    ON powersales.team_member_schedule (employee_id, working_date)
    INCLUDE (working_type, working_category1, attendance_log_id)
    WHERE employee_id IS NOT NULL;
