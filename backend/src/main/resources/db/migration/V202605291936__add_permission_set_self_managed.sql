-- Spec #837 — PermissionSet 자체 관리 (SF 독립 운용).
-- 1) permission_set_flags 에 is_locally_modified dirty 플래그 추가 — Stage1 재적재 보호용 (Stage1 service 변경은 후속).
-- 2) permission_set_change_log 변경 이력 테이블 신설 — SF SetupAuditTrail 동등 이상 audit.

-- ── 1) dirty 플래그 컬럼 ──────────────────────────────────────────────────
ALTER TABLE permission_set_flags
    ADD COLUMN is_locally_modified BOOLEAN NOT NULL DEFAULT FALSE;

-- ── 2) permission_set_change_log 테이블 ──────────────────────────────────
-- PS 삭제 시 본 row 의 permission_set_id 는 NULL 로 set (FK ON DELETE SET NULL — audit 보존).
CREATE TABLE permission_set_change_log (
    permission_set_change_log_id BIGSERIAL PRIMARY KEY,
    permission_set_id            BIGINT,
    event_type                   VARCHAR(32) NOT NULL,
    before_snapshot              JSONB,
    after_snapshot               JSONB,
    changed_by_id                BIGINT NOT NULL,
    changed_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason                VARCHAR(500),
    CONSTRAINT fk_permission_set_change_log_permission_set
        FOREIGN KEY (permission_set_id) REFERENCES permission_set(permission_set_id) ON DELETE SET NULL,
    CONSTRAINT fk_permission_set_change_log_changed_by
        FOREIGN KEY (changed_by_id) REFERENCES "user"(user_id)
);

CREATE INDEX idx_permission_set_change_log_permission_set_id
    ON permission_set_change_log(permission_set_id);

CREATE INDEX idx_permission_set_change_log_changed_at
    ON permission_set_change_log(changed_at DESC);

CREATE INDEX idx_permission_set_change_log_changed_by_id
    ON permission_set_change_log(changed_by_id);
