-- Spec #640 (P1-B): 관리자 웹 신규 거래처 등록 — ACCOUNT_WRITE 권한 신설.
-- 시스템관리자 / 영업지원실 두 역할에만 부여 (V2 기준 "10개 전체" 부여 받던 역할과 동일 정책).
-- 운영 정책에 따라 후속 추가/제거 가능.

INSERT INTO role_permission (role, permission) VALUES
    ('시스템관리자', 'ACCOUNT_WRITE'),
    ('영업지원실', 'ACCOUNT_WRITE');
