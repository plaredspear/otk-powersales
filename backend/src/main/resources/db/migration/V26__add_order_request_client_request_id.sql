-- Spec #592
-- order_request.client_request_id — 모바일 등록 멱등키. 동일 키 재요청 시 1차 row 의 응답을 그대로 반환.

ALTER TABLE powersales.order_request
    ADD COLUMN client_request_id VARCHAR(64);

CREATE UNIQUE INDEX idx_order_request_client_request_id_unique
    ON powersales.order_request (client_request_id)
    WHERE client_request_id IS NOT NULL;
