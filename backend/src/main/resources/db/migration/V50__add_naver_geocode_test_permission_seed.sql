-- Spec #638 P1-B: Naver Geocode 변환 테스트 권한 seed
-- SYSTEM_ADMIN(시스템관리자) role 에만 NAVER_GEOCODE_TEST 권한 자동 부여.
-- 기타 role 은 별도 권한 부여 화면(user_permission)에서 수동 부여 — 운영 도구는 SYSTEM_ADMIN 한정이 안전.

INSERT INTO role_permission (role, permission)
VALUES ('시스템관리자', 'NAVER_GEOCODE_TEST')
ON CONFLICT ON CONSTRAINT uq_role_permission DO NOTHING;
