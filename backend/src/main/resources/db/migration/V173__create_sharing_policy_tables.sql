-- spec #782 P1-B: SF Sharing Rule 정책 이식 — 정책 도메인 모델 7 테이블
-- (sharing_rule / sharing_rule_condition / sharing_rule_target / user_role_hierarchy_snapshot /
--  profile_flags / permission_set_flags / permission_set_assignment)

-- 1. sharing_rule — Sharing Rule 본문 1행
CREATE TABLE powersales.sharing_rule (
    sharing_rule_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    developer_name       VARCHAR(80)  NOT NULL,
    s_object_name        VARCHAR(80)  NOT NULL,
    rule_type            VARCHAR(20)  NOT NULL,
    label                VARCHAR(255),
    access_level         VARCHAR(10)  NOT NULL,
    include_owned_by_all BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_sharing_rule_developer_name_unique
    ON powersales.sharing_rule (developer_name);

CREATE INDEX idx_sharing_rule_s_object_name
    ON powersales.sharing_rule (s_object_name);

COMMENT ON TABLE  powersales.sharing_rule IS 'SF SharingRule meta 본문 1행 — spec #782 P1-B';
COMMENT ON COLUMN powersales.sharing_rule.rule_type IS 'CRITERIA / OWNER';
COMMENT ON COLUMN powersales.sharing_rule.access_level IS 'Read / Edit';

-- 2. sharing_rule_condition — Criteria-based rule 의 조건 1행
CREATE TABLE powersales.sharing_rule_condition (
    sharing_rule_condition_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sharing_rule_id           BIGINT       NOT NULL REFERENCES powersales.sharing_rule (sharing_rule_id) ON DELETE CASCADE,
    field                     VARCHAR(80)  NOT NULL,
    operator                  VARCHAR(20)  NOT NULL,
    -- value 는 H2 / 일부 DB reserved keyword 라 condition_value 로 회피.
    condition_value           TEXT,
    condition_order           INTEGER      NOT NULL,
    logic_connector           VARCHAR(10)
);

CREATE INDEX idx_sharing_rule_condition_rule_id
    ON powersales.sharing_rule_condition (sharing_rule_id);

CREATE UNIQUE INDEX idx_sharing_rule_condition_rule_order_unique
    ON powersales.sharing_rule_condition (sharing_rule_id, condition_order);

COMMENT ON COLUMN powersales.sharing_rule_condition.operator IS 'equals / notEqual / lessThan / greaterThan / lessOrEqual / greaterOrEqual / contains / notContain / startsWith / includes / excludes';
COMMENT ON COLUMN powersales.sharing_rule_condition.logic_connector IS 'AND / OR / NULL';

-- 3. sharing_rule_target — rule 의 대상 (Role / Group / User)
CREATE TABLE powersales.sharing_rule_target (
    sharing_rule_target_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sharing_rule_id        BIGINT       NOT NULL REFERENCES powersales.sharing_rule (sharing_rule_id) ON DELETE CASCADE,
    target_type            VARCHAR(30)  NOT NULL,
    target_sfid            VARCHAR(18),
    target_id              BIGINT
);

CREATE INDEX idx_sharing_rule_target_rule_id
    ON powersales.sharing_rule_target (sharing_rule_id);

CREATE INDEX idx_sharing_rule_target_sfid
    ON powersales.sharing_rule_target (target_sfid);

COMMENT ON COLUMN powersales.sharing_rule_target.target_type IS 'ROLE / ROLE_AND_SUBORDINATES / ROLE_AND_SUBORDINATES_INTERNAL / GROUP / USER';

-- 4. user_role_hierarchy_snapshot — UserRole 트리 정적 스냅샷
CREATE TABLE powersales.user_role_hierarchy_snapshot (
    user_role_id         BIGINT PRIMARY KEY REFERENCES powersales.user_role (user_role_id) ON DELETE CASCADE,
    all_subordinate_ids  JSONB        NOT NULL,
    depth                INTEGER      NOT NULL,
    ancestor_path        JSONB,
    snapshot_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_role_hierarchy_snapshot_depth
    ON powersales.user_role_hierarchy_snapshot (depth);

-- 5. profile_flags — Profile 의 system 권한 비트
CREATE TABLE powersales.profile_flags (
    profile_flags_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profile_id                  BIGINT  NOT NULL UNIQUE REFERENCES powersales.profile (profile_id) ON DELETE CASCADE,
    permissions_view_all_data   BOOLEAN NOT NULL DEFAULT FALSE,
    permissions_modify_all_data BOOLEAN NOT NULL DEFAULT FALSE,
    permissions_view_all_users  BOOLEAN NOT NULL DEFAULT FALSE,
    permissions_manage_users    BOOLEAN NOT NULL DEFAULT FALSE,
    permissions_api_enabled     BOOLEAN NOT NULL DEFAULT FALSE
);

-- 6. permission_set_flags — PermissionSet 의 object/system 권한 비트
CREATE TABLE powersales.permission_set_flags (
    permission_set_flags_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_set_sfid         VARCHAR(18)  NOT NULL UNIQUE,
    permission_set_name         VARCHAR(80)  NOT NULL,
    permissions_view_all_data   BOOLEAN      NOT NULL DEFAULT FALSE,
    permissions_modify_all_data BOOLEAN      NOT NULL DEFAULT FALSE,
    object_permissions          JSONB
);

COMMENT ON COLUMN powersales.permission_set_flags.object_permissions IS '{ "Account": { "viewAllRecords": true, "modifyAllRecords": false, ... }, ... }';

-- 7. permission_set_assignment — User ↔ PermissionSet 매핑
CREATE TABLE powersales.permission_set_assignment (
    permission_set_assignment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                         VARCHAR(18) UNIQUE,
    assignee_user_sfid           VARCHAR(18) NOT NULL,
    assignee_user_id             BIGINT      REFERENCES powersales."user" (user_id) ON DELETE CASCADE,
    permission_set_flags_id      BIGINT      NOT NULL REFERENCES powersales.permission_set_flags (permission_set_flags_id) ON DELETE CASCADE,
    is_active                    BOOLEAN     NOT NULL DEFAULT TRUE,
    assigned_at                  TIMESTAMPTZ
);

CREATE INDEX idx_permission_set_assignment_assignee_user_id
    ON powersales.permission_set_assignment (assignee_user_id);

CREATE INDEX idx_permission_set_assignment_active
    ON powersales.permission_set_assignment (is_active)
    WHERE is_active = TRUE;

CREATE UNIQUE INDEX idx_permission_set_assignment_user_set_unique
    ON powersales.permission_set_assignment (assignee_user_id, permission_set_flags_id)
    WHERE assignee_user_id IS NOT NULL;
