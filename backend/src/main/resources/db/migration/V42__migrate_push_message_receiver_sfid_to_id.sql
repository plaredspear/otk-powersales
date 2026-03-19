-- V42: PushMessageReceiver sfid 참조 → id(PK) 전환 + 테이블 리네이밍 (Spec #298)

-- Step 1: 테이블 리네이밍
ALTER TABLE pushmessagereceiver__c RENAME TO push_message_receiver;

-- Step 2: employeeId sfid → PK 전환
ALTER TABLE push_message_receiver ADD COLUMN employee_id BIGINT;

UPDATE push_message_receiver pmr
SET employee_id = e.id
FROM employee e
WHERE pmr.employeeid__c = e.sfid;

ALTER TABLE push_message_receiver DROP COLUMN employeeid__c;

-- Step 3: messageId sfid → PK 전환
ALTER TABLE push_message_receiver ADD COLUMN message_id INT;

UPDATE push_message_receiver pmr
SET message_id = pm.id
FROM pushmessage__c pm
WHERE pmr.messageid__c = pm.sfid;

ALTER TABLE push_message_receiver DROP COLUMN messageid__c;
