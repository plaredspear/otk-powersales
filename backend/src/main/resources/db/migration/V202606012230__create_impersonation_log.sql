-- 스펙 #851: Web 관리자 대행 로그인 (Impersonation) 감사 로그
-- 관리자가 다른 Web 사용자 계정을 대행한 시작/종료 이력을 적재 (적재 전용 — 조회 API 비범위).

CREATE TABLE powersales.impersonation_log (
    impersonation_log_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_user_id         BIGINT                      NOT NULL,
    target_user_id        BIGINT                      NOT NULL,
    reason                VARCHAR(500),
    started_at            TIMESTAMPTZ NOT NULL,
    ended_at              TIMESTAMPTZ,
    access_expires_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_impersonation_log_admin_user
        FOREIGN KEY (admin_user_id) REFERENCES powersales."user" (user_id),
    CONSTRAINT fk_impersonation_log_target_user
        FOREIGN KEY (target_user_id) REFERENCES powersales."user" (user_id)
);

CREATE INDEX idx_impersonation_log_admin_user_id ON powersales.impersonation_log (admin_user_id);
CREATE INDEX idx_impersonation_log_target_user_id ON powersales.impersonation_log (target_user_id);
CREATE INDEX idx_impersonation_log_started_at ON powersales.impersonation_log (started_at);
