-- V152: SF PermissionSetAssignment cut-over 적재용 staging 테이블.
--
-- 본 테이블은 scripts/sf-data-migration/ ETL 의 Stage 1 (raw 적재) 산출물 적재 대상.
-- ETL Stage 2 가 본 테이블 → user_permission 으로 매핑 변환.
--
-- 본 테이블의 row 자체는 데이터 마이그레이션 ETL 이 채우며 (Flyway 미활용 — 사용자가 psql 직접 실행),
-- 본 V152 는 테이블 구조만 정의 (스키마 변경은 Flyway 활용).
--
-- 런칭 + 안정화 후 폐기 가능 (별도 V{next}__drop_sf_permission_set_assignment_raw.sql).

CREATE TABLE IF NOT EXISTS powersales.sf_permission_set_assignment_raw (
    assignee_id VARCHAR(18) NOT NULL,
    assignee_employee_code VARCHAR(100),
    permission_set_id VARCHAR(18) NOT NULL,
    permission_set_name VARCHAR(80),
    permission_set_label VARCHAR(255),
    PRIMARY KEY (assignee_id, permission_set_id)
);

COMMENT ON TABLE powersales.sf_permission_set_assignment_raw IS
    'SF PermissionSetAssignment cut-over 적재용 staging — ETL Stage 1 산출 적재 대상. 런칭 + 안정화 후 폐기 검토';
