-- 여사원 현황 근무형태 필터(TeamMemberScheduleRepository.findEmployeeIdsByLatestWorkType) 가속화.
--
-- 문제: 사원별 '최근 출근등록 1건'을 DISTINCT ON (employee_id) ORDER BY working_date DESC, id DESC 로
--       뽑을 때, 기존 인덱스 idx_team_member_schedule_employee_id_working_date 는
--       (employee_id, working_date) 오름차순 + WHERE employee_id IS NOT NULL 이라
--       (a) attendance_log_id IS NOT NULL 필터, (b) working_date DESC/id DESC 정렬 순서를 커버하지 못한다.
--       결과적으로 team_member_schedule 전건(약 174만) Seq Scan + 디스크 external merge sort 로 22초+ (timeout).
--
-- 해결: DISTINCT ON 의 정렬 키와 정확히 정합하는 partial covering 인덱스를 추가한다.
--   - 정렬 정합: (employee_id ASC, working_date DESC, team_member_schedule_id DESC) → 인덱스를 앞에서
--     읽으며 각 employee_id 의 첫 행(=최근 1건)만 즉시 취해 DISTINCT ON 이 정렬 없이 완료된다.
--   - partial WHERE attendance_log_id IS NOT NULL: 출근등록분만(전체의 ~96%가 대상이나 미등록분 제외 +
--     쿼리 필터와 정합해 인덱스만으로 판정) → Seq Scan/필터 제거.
--   - INCLUDE (working_category1, working_category3): 근무형태 판정 컬럼을 인덱스에 실어 index-only scan
--     (힙 접근 제거).
CREATE INDEX idx_team_member_schedule_latest_worktype
    ON powersales.team_member_schedule (employee_id, working_date DESC, team_member_schedule_id DESC)
    INCLUDE (working_category1, working_category3)
    WHERE attendance_log_id IS NOT NULL;
