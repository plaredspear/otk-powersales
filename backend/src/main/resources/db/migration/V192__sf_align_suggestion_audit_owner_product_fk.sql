-- Suggestion (DKRetail__Proposal__c) SF Object 정합 보강.
--
-- 배경:
--   V173 으로 suggestion 테이블 신설 시 account/employee 만 FK + entity 매핑이 되어 있고,
--   SF describe 의 다른 reference 필드 (ProductId / OwnerId / CreatedById / LastModifiedById)
--   가 sfid mirror 도 없이 누락되어 있어 Stage2-A FK resolve 가 처리할 수 없는 비대칭 상태.
--
-- 적용 항목:
-- (1) DKRetail__ProductId__c — Lookup(Product) → product_id (Product FK) + product_sfid 는 V173 기존.
-- (2) OwnerId — Lookup(User, Group) R-2 polymorphic → owner_user_id / owner_group_id + XOR CHECK +
--     owner_sfid sync buffer 신설 (Promotion V132 동일 패턴).
-- (3) CreatedById — Lookup(User) → created_by_id (User FK) + created_by_sfid sync buffer 신설.
-- (4) LastModifiedById — Lookup(User) → last_modified_by_id (User FK) + last_modified_by_sfid sync buffer 신설.
--
-- 패턴 출처: V132 (Promotion 동일 변환), V123 (AttendanceLog).

-- (1) sfid sync buffer + FK id 컬럼 추가
ALTER TABLE powersales.suggestion
    ADD COLUMN product_id            BIGINT,
    ADD COLUMN owner_sfid            VARCHAR(18),
    ADD COLUMN owner_user_id         BIGINT,
    ADD COLUMN owner_group_id        BIGINT,
    ADD COLUMN created_by_sfid       VARCHAR(18),
    ADD COLUMN created_by_id         BIGINT,
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT;

-- (2) FK 제약
ALTER TABLE powersales.suggestion
    ADD CONSTRAINT fk_suggestion_product
        FOREIGN KEY (product_id) REFERENCES powersales.product (product_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_suggestion_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_suggestion_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_suggestion_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_suggestion_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (3) XOR CHECK — owner_user_id / owner_group_id 둘 다 채움 금지 (legacy 데이터 보존 위해 둘 다 null 허용)
ALTER TABLE powersales.suggestion
    ADD CONSTRAINT chk_suggestion_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (4) FK 인덱스
CREATE INDEX idx_suggestion_product_id            ON powersales.suggestion (product_id);
CREATE INDEX idx_suggestion_owner_user_id         ON powersales.suggestion (owner_user_id);
CREATE INDEX idx_suggestion_owner_group_id        ON powersales.suggestion (owner_group_id);
CREATE INDEX idx_suggestion_created_by_id         ON powersales.suggestion (created_by_id);
CREATE INDEX idx_suggestion_last_modified_by_id   ON powersales.suggestion (last_modified_by_id);
