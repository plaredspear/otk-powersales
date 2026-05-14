-- Promotion (DKRetail__Promotion__c) SF Object 정합 (sf-meta-diff DKRetail__Promotion__c.md).
--
-- 적용 항목:
-- (Q8) OwnerId polymorphic R-2 — referenceTo=[Group, User]. 단일 Employee FK → User/Group 분기 + XOR CHECK.
-- (Q6) CreatedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q7) LastModifiedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q2) Formula 컬럼 제거 — promotion_name (DKRetail__PromotionName__c). §6.7 위반.
-- (Q4) Formula 컬럼 제거 — actual_amount (ActualAmount__c, standard 네임스페이스). §6.7 위반. dk_actual_amount 일원화.
-- (Q5) Deprecated 컬럼 제거 — deprecated_acc_sfid (DKRetail__AccId__c, Label="사용안함"). E 분류 §6.2 자동 제외.
--
-- 패턴 출처: V123 (AttendanceLog 동일 변환).

-- (1) 기존 owner / audit FK 제약 + owner_id 컬럼 + idx 제거 (V82 명시 이름)
ALTER TABLE powersales.promotion
    DROP CONSTRAINT fk_promotion_owner,
    DROP CONSTRAINT fk_promotion_created_by,
    DROP CONSTRAINT fk_promotion_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_promotion_owner_id;

ALTER TABLE powersales.promotion
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.promotion
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.promotion
    ADD CONSTRAINT fk_promotion_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 허용)
ALTER TABLE powersales.promotion
    ADD CONSTRAINT chk_promotion_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (3) audit FK 재연결 (employee → user)
ALTER TABLE powersales.promotion
    ADD CONSTRAINT fk_promotion_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (4) FK 인덱스 (owner_user_id / owner_group_id 신규. created_by_id / last_modified_by_id 는 V82 에서 이미 생성 — 유지)
CREATE INDEX idx_promotion_owner_user_id  ON powersales.promotion (owner_user_id);
CREATE INDEX idx_promotion_owner_group_id ON powersales.promotion (owner_group_id);

-- (5) Formula 컬럼 + Deprecated 컬럼 제거
ALTER TABLE powersales.promotion
    DROP COLUMN promotion_name,
    DROP COLUMN actual_amount,
    DROP COLUMN deprecated_acc_sfid;
