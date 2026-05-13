-- Spec #710: PushMessageReceiver SF Object 정합 (Group A + Reference R-2)
-- CreatedById / LastModifiedById sfid 버퍼 + Employee FK 추가

ALTER TABLE powersales.push_message_receiver
    ADD COLUMN created_by_sfid  VARCHAR(18),
    ADD COLUMN created_by_id    BIGINT,
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT;

CREATE INDEX idx_push_message_receiver_created_by_id
    ON powersales.push_message_receiver (created_by_id);

CREATE INDEX idx_push_message_receiver_last_modified_by_id
    ON powersales.push_message_receiver (last_modified_by_id);
