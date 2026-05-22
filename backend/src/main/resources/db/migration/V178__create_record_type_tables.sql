-- spec #794: Record Type 별 분기 권한 — schema
--
-- 변경 사항:
--   1. record_type — sObject 별 RecordType 정의 (developerName / label / isActive)
--   2. profile_record_type — Profile 별 RecordType visibility (현재 운영 0건 — PermissionSet 위임)
--   3. permission_set_record_type — PermissionSet 별 RecordType visibility (운영 10건)
--
-- 인벤토리 §2.7 — 7 Custom SObject × 65 active RT (Promotion__c 27 / NewProduct__c 8 / ...)
-- Master RT 는 적재하지 않음 (Q4 옵션 1 — record_type_id IS NULL 이 곧 Master 의미)

-- ─────────────────────────────────────────────────────────
-- 1. record_type — RecordType 정의
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.record_type (
    record_type_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid            VARCHAR(18),
    sobject_name    VARCHAR(80) NOT NULL,
    developer_name  VARCHAR(80) NOT NULL,
    label           VARCHAR(255) NOT NULL,
    description     VARCHAR(1024),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT record_type_natural_key_unique UNIQUE (sobject_name, developer_name)
);

CREATE UNIQUE INDEX idx_record_type_sfid_unique
    ON powersales.record_type (sfid)
    WHERE sfid IS NOT NULL;

CREATE INDEX idx_record_type_sobject_name
    ON powersales.record_type (sobject_name);

COMMENT ON TABLE powersales.record_type IS
    'SF RecordType 정의 — spec #794. XML 출처: objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml. Master RT 는 적재하지 않음 (Q4 옵션 1)';

-- ─────────────────────────────────────────────────────────
-- 2. profile_record_type — Profile × RecordType visibility
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.profile_record_type (
    profile_record_type_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    profile_id                  BIGINT,
    profile_name                VARCHAR(255) NOT NULL,
    record_type_id              BIGINT,
    sobject_name                VARCHAR(80) NOT NULL,
    record_type_developer_name  VARCHAR(80) NOT NULL,
    visible                     BOOLEAN NOT NULL DEFAULT false,
    is_default                  BOOLEAN NOT NULL DEFAULT false,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_profile_record_type_profile_id
    ON powersales.profile_record_type (profile_id)
    WHERE profile_id IS NOT NULL;

CREATE INDEX idx_profile_record_type_record_type_id
    ON powersales.profile_record_type (record_type_id)
    WHERE record_type_id IS NOT NULL;

CREATE UNIQUE INDEX idx_profile_record_type_unique
    ON powersales.profile_record_type (profile_id, record_type_id)
    WHERE profile_id IS NOT NULL AND record_type_id IS NOT NULL;

COMMENT ON TABLE powersales.profile_record_type IS
    'Profile × RecordType visibility — spec #794. 운영 0건 (Profile 위임 패턴 — PermissionSet 측 사용)';

-- ─────────────────────────────────────────────────────────
-- 3. permission_set_record_type — PermissionSet × RecordType visibility
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.permission_set_record_type (
    permission_set_record_type_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_set_id             BIGINT,
    permission_set_name           VARCHAR(255) NOT NULL,
    record_type_id                BIGINT,
    sobject_name                  VARCHAR(80) NOT NULL,
    record_type_developer_name    VARCHAR(80) NOT NULL,
    visible                       BOOLEAN NOT NULL DEFAULT false,
    is_default                    BOOLEAN NOT NULL DEFAULT false,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_permission_set_record_type_ps_id
    ON powersales.permission_set_record_type (permission_set_id)
    WHERE permission_set_id IS NOT NULL;

CREATE INDEX idx_permission_set_record_type_rt_id
    ON powersales.permission_set_record_type (record_type_id)
    WHERE record_type_id IS NOT NULL;

CREATE UNIQUE INDEX idx_permission_set_record_type_unique
    ON powersales.permission_set_record_type (permission_set_id, record_type_id)
    WHERE permission_set_id IS NOT NULL AND record_type_id IS NOT NULL;

COMMENT ON TABLE powersales.permission_set_record_type IS
    'PermissionSet × RecordType visibility — spec #794. 운영 10건 (X1_1 / Marketing_ETC 등)';
