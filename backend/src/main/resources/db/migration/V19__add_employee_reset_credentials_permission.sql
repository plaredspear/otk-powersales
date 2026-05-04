-- Spec #582 P1-B: 사원 자격 정보 운영자 리셋 권한 추가
-- SYSTEM_ADMIN 단독으로 EMPLOYEE_RESET_CREDENTIALS 권한 부여 (운영 콘솔 단위 가시성으로 제어된
-- 레거시 SF 동등 정책)

INSERT INTO role_permission (role, permission)
VALUES ('시스템관리자', 'EMPLOYEE_RESET_CREDENTIALS')
ON CONFLICT ON CONSTRAINT uq_role_permission DO NOTHING;
