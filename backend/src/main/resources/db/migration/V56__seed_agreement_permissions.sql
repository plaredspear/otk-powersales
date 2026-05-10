-- Spec #658 (P1-B): 관리자 웹 약관 등록 — AGREEMENT_READ / AGREEMENT_WRITE 권한 신설.
-- legacy 운영 모델 (SF Setup 권한 보유자 한정) 정합 — SYSTEM_ADMIN 단일 역할 부여.
-- 운영 정책에 따라 후속 추가/제거 가능.

INSERT INTO role_permission (role, permission) VALUES
    ('SYSTEM_ADMIN', 'AGREEMENT_READ'),
    ('SYSTEM_ADMIN', 'AGREEMENT_WRITE')
ON CONFLICT ON CONSTRAINT uq_role_permission DO NOTHING;
