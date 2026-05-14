-- Group SF Object 정합 — RelatedId / OwnerId polymorphic R-2 User 분기 FK 전환 (sf-meta-diff Group.md Q1 / Q2).
--
-- 적용 항목:
-- (Q1) RelatedId polymorphic — referenceTo=[User, UserRole]. UserRole 은 backend 미매핑.
-- (Q2) OwnerId polymorphic   — referenceTo=[Organization, User]. Organization 은 backend 미매핑 (SF tenant 메타).
--
-- 패턴:
-- §5-4 R-2 polymorphic — 매핑 분기 (User) 는 FK 적용, 미매핑 분기 (UserRole / Organization) 는 sfid prefix 분기.
-- nullable FK 단독 분기이므로 CHECK XOR 불요 (FK 1개 + sfid prefix 가 분기 판정 역할).
-- 마이그레이션 도구 (SalesforceMigrationTool Phase 2) 가 `<관계>_sfid` 의 prefix `005` 일 때만 user.sfid lookup → FK 채움.
-- prefix `00D` (Organization) / `00E` (UserRole) 은 미매칭 → FK null 유지.
--
-- 패턴 출처: V112 (Appointment OwnerId polymorphic — 단 Group 은 한쪽 분기만 매핑).

ALTER TABLE powersales."group"
    ADD COLUMN related_user_id BIGINT,
    ADD COLUMN owner_user_id   BIGINT;

ALTER TABLE powersales."group"
    ADD CONSTRAINT fk_group_related_user
        FOREIGN KEY (related_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_group_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

CREATE INDEX idx_group_related_user_id ON powersales."group" (related_user_id);
CREATE INDEX idx_group_owner_user_id   ON powersales."group" (owner_user_id);
