-- Notice (DKRetail__Notice__c) SF Object 정합 — OwnerId polymorphic R-2 전환 (sf-meta-diff DKRetail__Notice__c.md Q1).
--
-- 적용 항목:
-- (Q1) OwnerId polymorphic R-2 — referenceTo=[Group, User]. 단일 User FK → User/Group 분기 + XOR CHECK.
--
-- 배경: V124 가 운영 데이터 (Apex grep Group set 0건) 근거로 단일 User FK 로 단순화했으나,
-- §6.4 v2.8 (2026-05-14) "SF 메타 권위 — 운영 데이터로 polymorphic 단순화 금지" 정책에 따라
-- referenceTo=[Group, User] 의 SF 메타 권위를 반영하여 polymorphic 분기 + CHECK XOR 로 정합.
--
-- 패턴 출처: V132 (Promotion 동일 변환).

-- (1) 기존 owner FK 제약 + owner_id 컬럼 + 인덱스 제거 (V76 + V124 잔존)
ALTER TABLE powersales.notice
    DROP CONSTRAINT fk_notice_owner;

DROP INDEX IF EXISTS powersales.idx_notice_owner_id;

ALTER TABLE powersales.notice
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.notice
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.notice
    ADD CONSTRAINT fk_notice_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_notice_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 허용)
ALTER TABLE powersales.notice
    ADD CONSTRAINT chk_notice_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (3) FK 인덱스 신설
CREATE INDEX idx_notice_owner_user_id  ON powersales.notice (owner_user_id);
CREATE INDEX idx_notice_owner_group_id ON powersales.notice (owner_group_id);
