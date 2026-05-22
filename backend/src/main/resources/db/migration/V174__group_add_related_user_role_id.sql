-- spec #782 P2-B: Group.RelatedId polymorphic 의 UserRole 분기 typed FK 추가.
-- 기존 `related_sfid` (polymorphic User+UserRole) + `related_user_id` (User 분기, #755 산출) 에
-- `related_user_role_id` 컬럼 (UserRole 분기) 추가. SF describe `_raw/Group.json` 의
-- `RelatedId.referenceTo = [User, UserRole]` 정합.
--
-- Stage 2 fk substep — Group.related_sfid prefix 005 → related_user_id, 00E → related_user_role_id 분기.
-- CHECK 제약 — User / UserRole 동시 NOT NULL 금지 (polymorphic).

ALTER TABLE powersales."group"
    ADD COLUMN related_user_role_id BIGINT NULL;

CREATE INDEX idx_group_related_user_role_id
    ON powersales."group" (related_user_role_id);

ALTER TABLE powersales."group"
    ADD CONSTRAINT fk_group_related_user_role_id
    FOREIGN KEY (related_user_role_id) REFERENCES powersales.user_role (user_role_id) ON DELETE SET NULL;

-- typed FK 2개 중 정확히 0 또는 1만 NOT NULL — SF describe 정합 (Group 자기참조 referenceTo 부재)
ALTER TABLE powersales."group"
    ADD CONSTRAINT chk_group_related_exclusive
    CHECK (
        (CASE WHEN related_user_id IS NOT NULL THEN 1 ELSE 0 END)
        + (CASE WHEN related_user_role_id IS NOT NULL THEN 1 ELSE 0 END)
        <= 1
    );

-- 기존 row backfill — related_sfid prefix 00E (UserRole) → related_user_role_id
UPDATE powersales."group" g
   SET related_user_role_id = ur.user_role_id
  FROM powersales.user_role ur
 WHERE g.related_sfid LIKE '00E%'
   AND ur.sfid = g.related_sfid;

COMMENT ON COLUMN powersales."group".related_user_role_id IS 'Type=Role/RoleAndSubordinates/RoleAndSubordinatesInternal 일 때 RelatedId → UserRole FK. SF describe referenceTo 정합 (spec #782 P2-B v1.4).';
