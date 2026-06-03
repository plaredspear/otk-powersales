-- 저장된 검색 (SavedSearch) — 목록 화면 검색 조건 프리셋 (Spec #852)
-- SF ListView 의 저장된 검색 개념을 신규 시스템에 도입한 범용 모듈. resource_key 로 화면 구분.

CREATE TABLE saved_search (
    id           BIGSERIAL    PRIMARY KEY,
    resource_key VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    scope        VARCHAR(20)  NOT NULL,
    owner_id     BIGINT       NOT NULL REFERENCES employee (employee_id),
    filters      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_saved_search_owner_name UNIQUE (resource_key, owner_id, scope, name)
);

CREATE INDEX idx_saved_search_resource_scope ON saved_search (resource_key, scope);
CREATE INDEX idx_saved_search_resource_owner ON saved_search (resource_key, owner_id);

COMMENT ON TABLE saved_search IS '저장된 검색 (목록 화면 검색 조건 프리셋) — Spec #852';
COMMENT ON COLUMN saved_search.resource_key IS '화면 식별자 (예: promotion)';
COMMENT ON COLUMN saved_search.scope IS 'PRIVATE(개인) | SHARED(공용)';
COMMENT ON COLUMN saved_search.owner_id IS '생성자 employee.id (SHARED 도 생성자 보존)';
COMMENT ON COLUMN saved_search.filters IS '필터 조건 (불투명 JSON, 화면별 키 구성 상이)';
