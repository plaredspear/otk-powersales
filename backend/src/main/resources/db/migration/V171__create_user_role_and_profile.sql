-- Spec #780 P1-B: SF UserRole / Profile entity 신규 시스템 편입.
--
-- UserRole (SF describe 실측 16 필드 중 7 필드 보존) + Profile (573 필드 중 8 필드 보존).
-- 본 entity 는 read-only audit lookup 용도. backend 권한 판정의 SoT 는 UserRole enum (auth/entity/UserRole.kt)
-- + ProfileType enum (user/entity/ProfileType.kt) — 변경 없음.

-- ============================================================
-- user_role 테이블
-- ============================================================
CREATE TABLE powersales.user_role (
    user_role_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                      VARCHAR(18) UNIQUE,
    name                      VARCHAR(80) NOT NULL,
    developer_name            VARCHAR(80),
    rollup_description        VARCHAR(80),
    parent_user_role_sfid     VARCHAR(18),
    parent_user_role_id       BIGINT,
    -- SF UserRole 은 CreatedDate / CreatedById 자체가 부재 — LastModifiedDate / LastModifiedById 만 보유
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    last_modified_by_sfid     VARCHAR(18),
    last_modified_by_id       BIGINT
);

-- 자기참조 FK — user_role 테이블 생성 후 ALTER 로 추가 (선언 순서 무관)
ALTER TABLE powersales.user_role
    ADD CONSTRAINT fk_user_role_parent_user_role
    FOREIGN KEY (parent_user_role_id) REFERENCES powersales.user_role (user_role_id)
    ON DELETE SET NULL;

ALTER TABLE powersales.user_role
    ADD CONSTRAINT fk_user_role_last_modified_by
    FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
    ON DELETE SET NULL;

CREATE INDEX idx_user_role_parent_user_role_id ON powersales.user_role (parent_user_role_id);
CREATE INDEX idx_user_role_last_modified_by_id ON powersales.user_role (last_modified_by_id);

-- ============================================================
-- profile 테이블
-- ============================================================
CREATE TABLE powersales.profile (
    profile_id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                      VARCHAR(18) UNIQUE,
    name                      VARCHAR(255) NOT NULL,
    user_type                 VARCHAR(40),
    description               VARCHAR(255),
    -- BaseEntity audit
    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    -- SF audit (CreatedById / LastModifiedById)
    created_by_sfid           VARCHAR(18),
    created_by_id             BIGINT,
    last_modified_by_sfid     VARCHAR(18),
    last_modified_by_id       BIGINT,
    CONSTRAINT fk_profile_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_profile_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_profile_created_by_id ON powersales.profile (created_by_id);
CREATE INDEX idx_profile_last_modified_by_id ON powersales.profile (last_modified_by_id);
