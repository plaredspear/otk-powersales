-- Spec #774: SF 인바운드 OAuth 2.0 인프라 audit 테이블.
-- SAP 측 sap_inbound_audit (V10) 와 격리된 별도 테이블 — 외부 시스템별 audit 분리.
-- previous_count 컬럼은 미포함 (SF inbound 는 chunked count 추적만 필요하므로 단순화).
CREATE TABLE sf_inbound_audit (
    sf_inbound_audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type          VARCHAR(30)  NOT NULL,
    client_id           VARCHAR(64),
    endpoint            VARCHAR(255),
    http_method         VARCHAR(10),
    client_ip           VARCHAR(45)  NOT NULL,
    scope               VARCHAR(500),
    received_count      INTEGER,
    reason              VARCHAR(1000),
    created_at          TIMESTAMP    NOT NULL
);

CREATE INDEX idx_sf_inbound_audit_client_created
    ON sf_inbound_audit (client_id, created_at DESC);

CREATE INDEX idx_sf_inbound_audit_event_created
    ON sf_inbound_audit (event_type, created_at DESC);
