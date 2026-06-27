-- 기간별 근무내역(개인) — findWorkHistoryForPeriod 조회 가속 인덱스.
--
-- WHERE: working_date BETWEEN ? AND ? (기간 범위, 수개월)
--        AND attendance_log_id IS NOT NULL  (출근 등록분만)
--        AND cost_center_code IN (?)        (지점 스코프 — 특정 지점 선택 위주)
--        AND (is_deleted IS NULL OR is_deleted = false)
--
-- 기존 team_member_schedule 인덱스는 전부 employee_id / account_id leading 이라
-- 본 쿼리(employee_id/account_id 미필터)는 인덱스를 못 타고 working_date 범위 Seq Scan 으로 떨어졌다.
-- (1.76M row / 2.9GB 테이블 — period-summary 응답 ~12s 정황과 일치.)
--
-- 설계:
--  - leading = cost_center_code (IN(?) eq 점프), 그다음 working_date (범위 좁힘) — 표준 (eq + range) 순서.
--  - partial WHERE attendance_log_id IS NOT NULL — 출근 미등록분 제외 + 인덱스 크기 축소 (본 쿼리 전용).
--  - INCLUDE — 집계(aggregate)가 쓰는 컬럼(working_type, working_category1, employee_id, account_id) +
--    is_deleted 판정 컬럼을 올려 힙 접근을 최소화한다.
--
-- 전사 권한자(cost_center_code 조건 없는 전사 조회)는 본 인덱스 leading eq 를 못 타지만,
-- 운영상 조회는 특정 지점 선택 위주이므로 cost_center_code leading 단일 인덱스로 충분하다.

CREATE INDEX idx_tms_cost_center_working_date_attend
    ON powersales.team_member_schedule (cost_center_code, working_date)
    INCLUDE (working_type, working_category1, employee_id, account_id, is_deleted)
    WHERE attendance_log_id IS NOT NULL;
