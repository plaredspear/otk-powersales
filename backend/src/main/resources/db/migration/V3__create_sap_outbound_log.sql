CREATE TABLE sap_outbound_log (
    sap_outbound_log_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    interface_id        VARCHAR(20)  NOT NULL,
    endpoint_path       VARCHAR(200) NOT NULL,
    request_count       INTEGER      NOT NULL,
    http_status         INTEGER,
    result_code         VARCHAR(10),
    result_msg          VARCHAR(500),
    attempt_count       INTEGER      NOT NULL,
    duration_ms         BIGINT       NOT NULL,
    error_detail        TEXT,
    requested_at        TIMESTAMP    NOT NULL,
    completed_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_sap_outbound_log_interface_id ON sap_outbound_log (interface_id);
CREATE INDEX idx_sap_outbound_log_requested_at ON sap_outbound_log (requested_at);
