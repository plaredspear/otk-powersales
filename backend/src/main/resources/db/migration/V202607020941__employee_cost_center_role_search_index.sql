-- Employee 목록/카운트 조회 복합 인덱스 신규 도입 — 여사원 현황 등 사원 목록 조회 가속.
--
-- 배경:
--   사원 목록 조회(EmployeeRepositoryCustomImpl.findEmployees) 는 6개 엔드포인트가 공유하며
--   WHERE (is_deleted IS NULL OR is_deleted = false) AND cost_center_code IN (...) AND role = ?
--   ORDER BY name ASC 형태로 페이지 + count 2회 조회한다.
--   기존 idx_employee_cost_center_code (V202606171850) 는 cost_center_code 선두만 커버해
--   role 필터는 heap filter, name 정렬은 매번 sort buffer 로 처리되었다.
--
-- 조치:
--   - (cost_center_code, role, is_deleted) 복합 인덱스로 세 필터를 인덱스 레벨에서 해소.
--   - name 을 INCLUDE 로 실어 정렬/페이지 content 를 index-only scan 으로 처리(heap 접근 최소화).
--
-- 비고:
--   - 선두 cost_center_code 는 기존 단일 인덱스를 상위 호환하지만, 단일 인덱스는 다른 조회 경로가
--     공용으로 쓰므로 유지한다(중복 제거는 별도 판단).
--   - keyword 필터는 containsIgnoreCase(LIKE '%...%') 라 인덱스 비대상이므로 포함하지 않는다.

CREATE INDEX idx_employee_cost_center_role_deleted
    ON powersales.employee (cost_center_code, role, is_deleted)
    INCLUDE (name);
