-- spec #795: Field-Level Security (FLS) — schema
--
-- 변경 사항:
--   1. profile_field_permission — Profile × Field 의 read/edit 권한 (운영 0건 — PermissionSet 위임)
--   2. permission_set_field_permission — PermissionSet × Field 의 read/edit 권한 (운영 26 PermissionSet)
--
-- 인벤토리 §2.8 — Profile 의 fieldPermissions 0건, PermissionSet 26건 활용 (Acc_Permission / Activity_View_All 등)

-- ─────────────────────────────────────────────────────────
-- 1. profile_field_permission
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.profile_field_permission (
    profile_field_permission_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profile_id                  BIGINT,
    profile_name                VARCHAR(255) NOT NULL,
    sobject_name                VARCHAR(80) NOT NULL,
    field_name                  VARCHAR(80) NOT NULL,
    readable                    BOOLEAN NOT NULL DEFAULT false,
    editable                    BOOLEAN NOT NULL DEFAULT false,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_profile_field_permission_profile_id
    ON powersales.profile_field_permission (profile_id)
    WHERE profile_id IS NOT NULL;

CREATE INDEX idx_profile_field_permission_sobject
    ON powersales.profile_field_permission (sobject_name);

CREATE UNIQUE INDEX idx_profile_field_permission_unique
    ON powersales.profile_field_permission (profile_id, sobject_name, field_name)
    WHERE profile_id IS NOT NULL;

COMMENT ON TABLE powersales.profile_field_permission IS
    'Profile × Field FLS — spec #795. 운영 0건 (PermissionSet 위임). XML 출처: profiles/<Name>.profile-meta.xml 의 <fieldPermissions>';

-- ─────────────────────────────────────────────────────────
-- 2. permission_set_field_permission
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.permission_set_field_permission (
    permission_set_field_permission_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_set_id                  BIGINT,
    permission_set_name                VARCHAR(255) NOT NULL,
    sobject_name                       VARCHAR(80) NOT NULL,
    field_name                         VARCHAR(80) NOT NULL,
    readable                           BOOLEAN NOT NULL DEFAULT false,
    editable                           BOOLEAN NOT NULL DEFAULT false,
    created_at                         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_permission_set_field_permission_ps_id
    ON powersales.permission_set_field_permission (permission_set_id)
    WHERE permission_set_id IS NOT NULL;

CREATE INDEX idx_permission_set_field_permission_sobject
    ON powersales.permission_set_field_permission (sobject_name);

CREATE UNIQUE INDEX idx_permission_set_field_permission_unique
    ON powersales.permission_set_field_permission (permission_set_id, sobject_name, field_name)
    WHERE permission_set_id IS NOT NULL;

COMMENT ON TABLE powersales.permission_set_field_permission IS
    'PermissionSet × Field FLS — spec #795. 운영 26 PermissionSet. XML 출처: permissionsets/<Name>.permissionset-meta.xml 의 <fieldPermissions>';
