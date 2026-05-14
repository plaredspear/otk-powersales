-- AttendanceLog (DKRetail__CommuteLog__c) OwnerId polymorphic R-2 + audit FK User 전환.
--
-- SF `DKRetail__CommuteLog__c.OwnerId.referenceTo = [Group, User]` polymorphic 의 R-2 정합 +
-- CreatedById / LastModifiedById (`referenceTo = [User]`) audit FK 의 Employee → User 전환.
--
-- 패턴 출처: V122 (AttendInfo 동일 변환).
--
-- (1) 기존 audit / owner FK 제약 + owner_id 컬럼 + idx 제거 (V74 inline FK)
-- (2) owner_user_id (User FK) + owner_group_id (Group FK) 컬럼 추가 + XOR CHECK
-- (3) created_by_id / last_modified_by_id FK 를 user 로 재연결
-- (4) FK 인덱스 (owner_user_id / owner_group_id 신규)

-- (1) 기존 제약 + 컬럼 + 인덱스 제거 (V74 명시 이름)
ALTER TABLE powersales.attendance_log
    DROP CONSTRAINT fk_attendance_log_owner,
    DROP CONSTRAINT fk_attendance_log_created_by,
    DROP CONSTRAINT fk_attendance_log_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_attendance_log_owner_id;

ALTER TABLE powersales.attendance_log
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.attendance_log
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.attendance_log
    ADD CONSTRAINT fk_attendance_log_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attendance_log_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.attendance_log
    ADD CONSTRAINT chk_attendance_log_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (3) audit FK 재연결 (employee → user)
ALTER TABLE powersales.attendance_log
    ADD CONSTRAINT fk_attendance_log_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_attendance_log_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (4) FK 인덱스 (created_by_id / last_modified_by_id 인덱스는 V74 에서 이미 생성 — 유지)
CREATE INDEX idx_attendance_log_owner_user_id  ON powersales.attendance_log (owner_user_id);
CREATE INDEX idx_attendance_log_owner_group_id ON powersales.attendance_log (owner_group_id);
