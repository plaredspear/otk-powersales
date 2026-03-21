-- PushMessage PK 및 레거시 컬럼명 정리 (#364)
ALTER TABLE push_message RENAME COLUMN id TO push_message_id;
ALTER TABLE push_message RENAME COLUMN "message__c" TO message;
ALTER TABLE push_message RENAME COLUMN "scheduledate__c" TO schedule_date;
ALTER TABLE push_message RENAME COLUMN isdeleted TO is_deleted;
