-- spec #790: SF Sharing 메타 데이터 적재 — schema 정합
--
-- 변경 사항:
--   1. user_role_hierarchy_snapshot — Stage 1 적재용 developer_name / parent_developer_name 컬럼 추가
--      + user_role_id PK NOT NULL 해제 (적재 시점에 NULL, Stage 2 fk resolve 후 채움)
--      + all_subordinate_ids / depth NOT NULL 해제 (적재 시점에 NULL, hierarchy 재계산 후 채움)
--   2. sharing_rule_target — target_developer_name 컬럼 + (sharing_rule_id, target_type, target_sfid) UNIQUE
--   3. profile_flags — profile_name 컬럼 추가 + profile_id NOT NULL 해제
--   4. group_member — 신규 테이블 (PublicGroup 멤버십)
--
-- 본 마이그레이션은 V175 의 schema 를 ALTER 로 보완. V175 본문은 수정 금지 (CLAUDE.md 정책).

-- ─────────────────────────────────────────────────────────
-- 1. user_role_hierarchy_snapshot — Stage 1 적재용 자연 키 추가
-- ─────────────────────────────────────────────────────────

-- 1-1. PK 컬럼 nullable 변경 — Stage 1 적재 시 user_role_id NULL 허용 (Stage 2 fk resolve 후 채움).
--     PK 제약은 유지 (NOT NULL 효과는 NULL 허용으로 약화) — PostgreSQL 의 PRIMARY KEY 는
--     자동 NOT NULL 이라 ALTER 로 NOT NULL 만 분리할 수 없음. 대안: PRIMARY KEY 제거 + UNIQUE 로 약화.
ALTER TABLE powersales.user_role_hierarchy_snapshot
    DROP CONSTRAINT user_role_hierarchy_snapshot_pkey;

ALTER TABLE powersales.user_role_hierarchy_snapshot
    ALTER COLUMN user_role_id DROP NOT NULL;

ALTER TABLE powersales.user_role_hierarchy_snapshot
    ADD CONSTRAINT user_role_hierarchy_snapshot_user_role_id_unique UNIQUE (user_role_id);

-- 1-2. all_subordinate_ids / depth — 적재 시점에 NULL 허용 (hierarchy 재계산 후 채움)
ALTER TABLE powersales.user_role_hierarchy_snapshot
    ALTER COLUMN all_subordinate_ids DROP NOT NULL;

ALTER TABLE powersales.user_role_hierarchy_snapshot
    ALTER COLUMN depth DROP NOT NULL;

-- 1-3. developer_name / parent_developer_name / name / description / may_forecast_manager_share — XML 메타 출처
ALTER TABLE powersales.user_role_hierarchy_snapshot
    ADD COLUMN developer_name             VARCHAR(80),
    ADD COLUMN parent_developer_name      VARCHAR(80),
    ADD COLUMN name                       VARCHAR(255),
    ADD COLUMN description                VARCHAR(1024),
    ADD COLUMN may_forecast_manager_share BOOLEAN;

CREATE UNIQUE INDEX idx_user_role_hierarchy_snapshot_developer_name_unique
    ON powersales.user_role_hierarchy_snapshot (developer_name)
    WHERE developer_name IS NOT NULL;

COMMENT ON COLUMN powersales.user_role_hierarchy_snapshot.developer_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 user_role.developer_name → user_role_id 채움';

-- ─────────────────────────────────────────────────────────
-- 2. sharing_rule_target — target_developer_name + UNIQUE
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.sharing_rule_target
    ADD COLUMN target_developer_name VARCHAR(80);

COMMENT ON COLUMN powersales.sharing_rule_target.target_developer_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 UserRole/Group developer_name → target_sfid + target_id 채움';

-- 재적재 멱등 — (sharing_rule_id, target_type, target_sfid) UNIQUE (target_sfid NULL 허용)
CREATE UNIQUE INDEX idx_sharing_rule_target_unique
    ON powersales.sharing_rule_target (sharing_rule_id, target_type, target_sfid)
    WHERE target_sfid IS NOT NULL;

-- ─────────────────────────────────────────────────────────
-- 3. profile_flags — profile_name + profile_id NOT NULL 해제
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.profile_flags
    ALTER COLUMN profile_id DROP NOT NULL;

ALTER TABLE powersales.profile_flags
    ADD COLUMN profile_name VARCHAR(255);

CREATE UNIQUE INDEX idx_profile_flags_profile_name_unique
    ON powersales.profile_flags (profile_name)
    WHERE profile_name IS NOT NULL;

COMMENT ON COLUMN powersales.profile_flags.profile_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 profile.name → profile_id 채움';

-- ─────────────────────────────────────────────────────────
-- 4. group_member — PublicGroup 멤버십 테이블 신설
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.group_member (
    group_member_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                VARCHAR(18) NOT NULL UNIQUE,
    group_sfid          VARCHAR(18) NOT NULL,
    group_id            BIGINT,
    user_or_group_sfid  VARCHAR(18) NOT NULL,
    user_or_group_id    BIGINT,
    user_or_group_type  VARCHAR(10),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_group_member_group_id
    ON powersales.group_member (group_id)
    WHERE group_id IS NOT NULL;

CREATE INDEX idx_group_member_user_or_group_id
    ON powersales.group_member (user_or_group_id, user_or_group_type)
    WHERE user_or_group_id IS NOT NULL;

-- 재적재 멱등 — (group_id, user_or_group_id) UNIQUE (Stage 2 fk resolve 완료 후 활성)
CREATE UNIQUE INDEX idx_group_member_unique
    ON powersales.group_member (group_id, user_or_group_id)
    WHERE group_id IS NOT NULL AND user_or_group_id IS NOT NULL;

COMMENT ON TABLE powersales.group_member IS
    'SF GroupMember mirror — spec #790. Group.Type = Regular (PublicGroup) 멤버십. user_or_group_type 은 prefix 005/00G 분기로 USER/GROUP 박제';

COMMENT ON COLUMN powersales.group_member.user_or_group_sfid IS
    'SF UserOrGroupId polymorphic (005 = User / 00G = Group). Stage 2 fk resolve 가 user_or_group_id + user_or_group_type 채움';
