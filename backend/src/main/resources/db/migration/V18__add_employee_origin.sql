-- Spec #579: Employee origin 컬럼 추가 (SAP 동기화 직원 vs 수동 등록 관리자 구분)
--
-- - SAP : SAP 인바운드로 등록·갱신되는 직원 (기본값)
-- - MANUAL : Web Admin 에서 수동 등록된 시스템 관리자
--
-- 기존 모든 행은 DEFAULT 'SAP' 로 자동 백필된다.

ALTER TABLE powersales.employee
    ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'SAP';

ALTER TABLE powersales.employee
    ADD CONSTRAINT employee_origin_check
        CHECK (origin IN ('SAP', 'MANUAL'));

CREATE INDEX idx_employee_origin
    ON powersales.employee (origin);
