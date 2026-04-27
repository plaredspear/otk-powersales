CREATE TABLE sap_inbound_audit (
    sap_inbound_audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type           VARCHAR(30)  NOT NULL,
    client_id            VARCHAR(64),
    endpoint             VARCHAR(255),
    http_method          VARCHAR(10),
    client_ip            VARCHAR(45)  NOT NULL,
    scope                VARCHAR(500),
    received_count       INTEGER,
    previous_count       INTEGER,
    reason               VARCHAR(1000),
    created_at           TIMESTAMP    NOT NULL
);

CREATE INDEX idx_sap_inbound_audit_client_created
    ON sap_inbound_audit (client_id, created_at DESC);

CREATE INDEX idx_sap_inbound_audit_event_created
    ON sap_inbound_audit (event_type, created_at DESC);
