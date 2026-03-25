-- V57: PushMessageReceiver sfid 컬럼 추가 + message_id → push_message_id 리네이밍 (427)

ALTER TABLE push_message_receiver
    ADD COLUMN employee_sfid VARCHAR(18),
    ADD COLUMN push_message_sfid VARCHAR(18);

ALTER TABLE push_message_receiver
    RENAME COLUMN message_id TO push_message_id;
