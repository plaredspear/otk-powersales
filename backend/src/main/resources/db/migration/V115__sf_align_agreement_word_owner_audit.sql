-- AgreementWord.OwnerId polymorphic R-2 + audit FK User 전환 (sf-meta-diff Q4 + Q5).
--
-- SF `AgreementWord__c.OwnerId.referenceTo = [Group, User]` polymorphic 의 R-2 정합 +
-- CreatedById / LastModifiedById (`referenceTo = [User]`) audit FK 의 Employee → User 전환.
--
-- 패턴 출처: spec #755 (Appointment.OwnerId polymorphic) + spec #757 (User entity) +
-- V114 (AgreementHistory 동일 변환).
--
-- (1) 기존 audit / owner FK 제약 + owner_id 컬럼 + idx 제거 (V62 inline FK 로 생성된 자동 이름)
-- (2) owner_user_id (User FK) + owner_group_id (Group FK) 컬럼 추가 + XOR CHECK
-- (3) created_by_id / last_modified_by_id FK 를 user 로 재연결
-- (4) FK 인덱스 (owner_user_id / owner_group_id 신규)

-- (1) 기존 제약 + 컬럼 + 인덱스 제거 (V62 inline FK 로 생성된 자동 이름)
ALTER TABLE powersales.agreement_word
    DROP CONSTRAINT agreement_word_owner_id_fkey,
    DROP CONSTRAINT agreement_word_created_by_id_fkey,
    DROP CONSTRAINT agreement_word_last_modified_by_id_fkey;

DROP INDEX IF EXISTS powersales.idx_agreement_word_owner_id;

ALTER TABLE powersales.agreement_word
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.agreement_word
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.agreement_word
    ADD CONSTRAINT fk_agreement_word_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_agreement_word_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.agreement_word
    ADD CONSTRAINT chk_agreement_word_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (3) audit FK 재연결 (employee → user)
ALTER TABLE powersales.agreement_word
    ADD CONSTRAINT fk_agreement_word_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_agreement_word_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (4) FK 인덱스 (created_by_id / last_modified_by_id 인덱스는 V62 에서 이미 생성 — 유지)
CREATE INDEX idx_agreement_word_owner_user_id  ON powersales.agreement_word (owner_user_id);
CREATE INDEX idx_agreement_word_owner_group_id ON powersales.agreement_word (owner_group_id);
