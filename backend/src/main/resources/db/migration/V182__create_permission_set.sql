-- spec #796 — PermissionSet 정규 테이블 신설.
--
-- 기존 V175 의 permission_set_flags 는 "객체 권한 비트 (object_permissions JSONB)" 만 보관하는 역할.
-- 본 V182 는 PermissionSet 자체 (name / label / description 메타) 를 별도 테이블로 분리.
--
-- 동기:
--   - permission_set_record_type / permission_set_field_permission 의 permission_set_id FK 가
--     실제로 가리킬 대상 테이블이 필요 (spec #794, #795 의 SfFkResolveTables NATURAL_KEY_FK_MAPPINGS 참조처).
--   - permission_set_flags 와는 1:1 관계 (한 PermissionSet 당 한 flags row).

CREATE TABLE powersales.permission_set (
    permission_set_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid               VARCHAR(18) UNIQUE,
    name               VARCHAR(80) NOT NULL UNIQUE,
    label              VARCHAR(255),
    description        VARCHAR(1024),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE powersales.permission_set IS
    'PermissionSet 정규 테이블 — spec #796. XML 출처: permissionsets/<Name>.permissionset-meta.xml. permission_set_flags 와 1:1 (flags 는 object_permissions JSONB).';

-- permission_set_flags.permission_set_id FK 추가 — Stage2 fk resolve 후 채움
ALTER TABLE powersales.permission_set_flags
    ADD COLUMN permission_set_id BIGINT REFERENCES powersales.permission_set (permission_set_id) ON DELETE CASCADE;

CREATE INDEX idx_permission_set_flags_permission_set_id
    ON powersales.permission_set_flags (permission_set_id)
    WHERE permission_set_id IS NOT NULL;

CREATE UNIQUE INDEX idx_permission_set_flags_permission_set_id_unique
    ON powersales.permission_set_flags (permission_set_id)
    WHERE permission_set_id IS NOT NULL;
