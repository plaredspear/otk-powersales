-- spec #792: Sharing Recalc admin endpoint — audit log
--
-- sharing_recalc_log — admin 이 sharing recalc endpoint 를 호출한 이력 audit.
-- 본 테이블은 SF mirror 아닌 신규 시스템 자체 audit 데이터 (Stage1Targets ALL 미등록).

CREATE TABLE powersales.sharing_recalc_log (
    sharing_recalc_log_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    triggered_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    triggered_by_user_id  BIGINT NOT NULL,
    scope                 VARCHAR(50) NOT NULL,
    sobject_name          VARCHAR(80),
    evicted_cache_count   INT NOT NULL DEFAULT 0,
    duration_ms           INT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sharing_recalc_log_scope_check CHECK (scope IN ('ALL', 'SOBJECT'))
);

CREATE INDEX idx_sharing_recalc_log_triggered_at
    ON powersales.sharing_recalc_log (triggered_at DESC);

CREATE INDEX idx_sharing_recalc_log_triggered_by_user_id
    ON powersales.sharing_recalc_log (triggered_by_user_id);

COMMENT ON TABLE powersales.sharing_recalc_log IS
    'spec #792 — sharing recalc admin endpoint 호출 audit. 신규 시스템 자체 audit (SF mirror 아님)';

COMMENT ON COLUMN powersales.sharing_recalc_log.scope IS
    'ALL = recalc/all 호출, SOBJECT = recalc/sobject/{name} 호출';
