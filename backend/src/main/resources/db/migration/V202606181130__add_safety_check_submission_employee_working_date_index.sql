-- 안전점검 제출 조회 키 복합 인덱스 신규 도입 — 안전점검 현황 조회 가속.
--
-- 배경:
--   안전점검 현황(AdminSafetyCheckService.getStatus) 및 모바일 제출/중복검증
--   (SafetyCheckService) 의 safety_check_submission 조회는 모두
--     (employee_id [IN], working_date) 조합을 WHERE 키로 사용한다.
--       - findByEmployeeIdInAndWorkingDate (admin 현황 벌크 조회)
--       - findByEmployeeIdAndWorkingDate / existsByEmployeeIdAndWorkingDate (모바일)
--   기존 인덱스는 idx_safety_check_submission_employee_id (employee_id 단일, V8) 뿐이라
--   employee_id 로만 행을 추린 뒤 working_date 는 후속 필터로 평가되었다.
--
-- 조치:
--   - (employee_id, working_date) 복합 인덱스 신규 추가. 세 조회 메소드 모두 employee_id 가
--     선두 등치/IN 조건이고 working_date 가 등치 조건이라 이 컬럼 순서가 최적.
--   - 신규 복합 인덱스의 선두 컬럼이 employee_id 로 동일하여 기존 단일 인덱스의 역할을
--     완전히 포함하므로, 중복 인덱스인 idx_safety_check_submission_employee_id 를 제거한다.
--
-- 비고:
--   - safety_check_submission 에는 employee_id 단독 조회 경로가 없어(Repository 3개 메소드 모두
--     working_date 동반) 단일 인덱스 제거는 안전하다.

CREATE INDEX idx_safety_check_submission_employee_id_working_date
    ON powersales.safety_check_submission (employee_id, working_date);

DROP INDEX IF EXISTS powersales.idx_safety_check_submission_employee_id;
