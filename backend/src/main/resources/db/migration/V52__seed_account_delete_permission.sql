-- Spec #642 (P1-B): 관리자 웹 거래처 삭제 — ACCOUNT_DELETE 권한 신설.
-- 삭제는 등록보다 보수적으로 운영 — SYSTEM_ADMIN 단일 역할에만 부여.
-- 운영 정책에 따라 후속 추가/제거 가능.

INSERT INTO role_permission (role, permission) VALUES
    ('SYSTEM_ADMIN', 'ACCOUNT_DELETE')
ON CONFLICT ON CONSTRAINT uq_role_permission DO NOTHING;
