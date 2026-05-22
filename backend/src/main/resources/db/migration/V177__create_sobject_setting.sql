-- spec #791: SF OWD (Org-Wide Default) + Owner default access — 정합 schema
--
-- 변경 사항:
--   1. sobject_setting — SObject 별 OWD (Org-Wide Default) + role hierarchy 옵트인 메타.
--      XML 메타 3 출처 정규화 (Custom <sharingModel> / Standard Sharing.settings-meta.xml 의
--      <sharingSettings> + <sharingHierarchy>). #790 의 7 entity 와 동일 XML 출처 패턴.
--   2. sobject_relation — Q2 옵션 1: master-detail relationship 정규화 메타.
--      ControlledByParent SObject 의 parent SObject 매핑 (SF describe `childRelationships` 동등).

-- ─────────────────────────────────────────────────────────
-- 1. sobject_setting — OWD + hierarchy 옵트인
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.sobject_setting (
    sobject_setting_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sobject_name          VARCHAR(80) NOT NULL UNIQUE,
    org_wide_default      VARCHAR(30) NOT NULL,
    allow_hierarchy_grant BOOLEAN NOT NULL DEFAULT true,
    parent_sobject_name   VARCHAR(80),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sobject_setting_owd_check CHECK (
        org_wide_default IN ('Private', 'PublicReadOnly', 'PublicReadWrite', 'ControlledByParent', 'Read', 'ReadWrite')
    )
);

CREATE INDEX idx_sobject_setting_org_wide_default
    ON powersales.sobject_setting (org_wide_default);

CREATE INDEX idx_sobject_setting_allow_hierarchy_grant
    ON powersales.sobject_setting (allow_hierarchy_grant)
    WHERE allow_hierarchy_grant = true;

COMMENT ON TABLE powersales.sobject_setting IS
    'SF OWD (Org-Wide Default) + role hierarchy 옵트인 메타 — spec #791. XML 메타 3 출처 정규화';

COMMENT ON COLUMN powersales.sobject_setting.org_wide_default IS
    'SF sharingModel — Private / PublicReadOnly / PublicReadWrite / ControlledByParent. Custom SObject 운영에서 Read / ReadWrite 도 발견 (인벤토리 §2.6) → CHECK 에 포함';

COMMENT ON COLUMN powersales.sobject_setting.allow_hierarchy_grant IS
    'Sharing.settings-meta.xml 의 <sharingHierarchy>.grantAccessUsingHierarchies. SF 기본값 true. SF 운영의 User / UserRole 같은 일부 SObject 는 false';

COMMENT ON COLUMN powersales.sobject_setting.parent_sobject_name IS
    'ControlledByParent SObject 의 부모 SObject API 명. 본 컬럼은 보조 메타 — 정확한 FK 관계는 sobject_relation 테이블 (Q2 옵션 1)';

-- ─────────────────────────────────────────────────────────
-- 2. sobject_relation — master-detail relationship 정규화 메타
-- ─────────────────────────────────────────────────────────

CREATE TABLE powersales.sobject_relation (
    sobject_relation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    child_sobject_name  VARCHAR(80) NOT NULL,
    parent_sobject_name VARCHAR(80) NOT NULL,
    relation_field_name VARCHAR(80) NOT NULL,
    is_master_detail    BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sobject_relation_unique UNIQUE (child_sobject_name, relation_field_name)
);

CREATE INDEX idx_sobject_relation_child
    ON powersales.sobject_relation (child_sobject_name);

CREATE INDEX idx_sobject_relation_parent
    ON powersales.sobject_relation (parent_sobject_name);

COMMENT ON TABLE powersales.sobject_relation IS
    'SF master-detail relationship 정규화 메타 — spec #791 Q2 옵션 1. ControlledByParent SObject 의 parent 추론 출처';

COMMENT ON COLUMN powersales.sobject_relation.relation_field_name IS
    'child SObject 의 FK 필드 API 명 (예: DKRetail__Promotion__c 의 master-detail FK)';
