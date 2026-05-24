-- push_message_receiver audit FK 정합 — Employee → User
--
-- 배경:
--   V65 가 push_message_receiver 에 created_by_id / last_modified_by_id BIGINT 컬럼을 추가하면서
--   FK 제약을 누락. Entity 측은 createdBy: Employee 매핑이라 Stage2 SF Resolve 가 적재 시
--   User.user_id 를 박는 시점에 FK violation 은 없으나 entity ORM 동작이 의미 mismatch.
--
-- SF 권위 (describe API dump):
--   PushMessageReceiver__c.CreatedById.referenceTo = [User]
--   PushMessageReceiver__c.LastModifiedById.referenceTo = [User]
--
-- 패턴 (V200 audit Employee→User 일괄 정합과 동일):
--   (a) 기존 잔여값 NULL 초기화 (Employee.id 였을 가능성 — User.user_id 의미와 불일치)
--   (b) User FK 제약 추가 (ON DELETE SET NULL)
--   FK 제약이 부재했으므로 DROP CONSTRAINT 불요.

BEGIN;

UPDATE powersales.push_message_receiver
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.push_message_receiver
    ADD CONSTRAINT fk_push_message_receiver_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_push_message_receiver_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- 인덱스는 V65 가 이미 생성 (idx_push_message_receiver_created_by_id /
-- idx_push_message_receiver_last_modified_by_id) — 재생성 불요.

COMMIT;
