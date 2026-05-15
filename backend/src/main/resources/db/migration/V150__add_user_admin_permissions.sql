-- V150: USER_READ / USER_WRITE 권한 시드.
--
-- web admin 의 User 엔티티 관리 화면 (목록 조회 / 비밀번호 리셋 / 활성-비활성 토글)
-- 접근 제어용 권한 2종을 SYSTEM_ADMIN 역할에만 부여한다.
--
-- USER_READ  : GET /api/v1/admin/users, GET /api/v1/admin/users/{id}
-- USER_WRITE : POST /api/v1/admin/users/{id}/reset-password,
--              PUT  /api/v1/admin/users/{id}/active

INSERT INTO role_permission (role, permission) VALUES
    ('SYSTEM_ADMIN', 'USER_READ'),
    ('SYSTEM_ADMIN', 'USER_WRITE')
ON CONFLICT ON CONSTRAINT uq_role_permission DO NOTHING;
