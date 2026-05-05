-- 스펙 #588: SAP outbound 송신(출근/진열 마스터)을 위해 V4 에서 drop 된
-- sap_outbound_log 테이블을 재생성한다. 컬럼 정의는 V3 와 동일하다.
-- 페이지 단위 SAP 호출 결과(성공/실패)를 본 테이블에 적재한다.

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
