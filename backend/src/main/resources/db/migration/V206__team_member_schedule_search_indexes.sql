-- team_member_schedule 조회 패턴 ((employee_id|account_id) eq + working_date range) 가속화.
-- 사전 분석: 1.76M row / 2.9GB / employee_id NULL 30% (537,955 row) / 4.5년 working_date 범위.
-- 본 인덱스 없이 employee_id=? + working_date BETWEEN 1개월 조회가 Parallel Seq Scan 11.8s.
-- composite (eq + range) 패턴은 leading eq column → range column 순서가 표준.

-- 1) employee_id 단일 차원 + working_date 범위 — 본 프로젝트 가장 빈번한 schedule 조회 패턴.
--    findMonthlyByEmployeeIds / findActiveByEmployeeIdAndDate / findByEmployeeIdAndWorkingDate /
--    findWorkSchedulesByEmployeeAndAccountAndMonth / deleteAnnualLeaveByEmployeeIdAndDateRange /
--    findAnnualLeaveByDateRangeAndEmployeeIds / findDistinctAccountIdsByEmployeeIdAndDateRange /
--    countWorkSchedulesByEmployeeAndDateAndWorkingType 8개 쿼리 수혜.
--    employee_id NULL 30% row 는 partial 로 제외 (employee 차원 조회 대상 아님).
CREATE INDEX idx_team_member_schedule_employee_id_working_date
    ON powersales.team_member_schedule (employee_id, working_date)
    WHERE employee_id IS NOT NULL;

-- 2) account_id 단일 차원 + working_date 범위 — findMonthlyByAccountIds 의 황금 인덱스.
--    account_id NULL row 도 partial 로 제외.
CREATE INDEX idx_team_member_schedule_account_id_working_date
    ON powersales.team_member_schedule (account_id, working_date)
    WHERE account_id IS NOT NULL;
