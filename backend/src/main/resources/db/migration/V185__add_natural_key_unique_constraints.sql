-- Stage 2 fk-natural-key UPDATE 의 ref 자연 키 컬럼에 UNIQUE 제약 보강.
--
-- 배경:
--   NaturalKeyFkSpec 의 ref 자연 키 컬럼은 1:1 lookup 을 전제로 하지만 V171 의 schema 정의에
--   UNIQUE 제약이 누락되어 있었음. SF org 차원에서 자체 UNIQUE 보장하지만 DB constraint 가
--   보호하지 않으면 defense-in-depth 결격.
--
-- 변경 사항:
--   1. profile.name UNIQUE — ProfileFlags / ProfileRecordType / ProfileFieldPermission lookup 의 ref
--   2. user_role.developer_name UNIQUE — UserRoleHierarchySnapshot lookup 의 ref
--      (nullable 컬럼이라 partial UNIQUE INDEX 사용)
--
-- 본 마이그레이션은 V171 의 schema 를 ALTER 로 보완. V171 본문은 수정 금지 (CLAUDE.md 정책).

-- ─────────────────────────────────────────────────────────
-- 1. profile.name UNIQUE — V171 line 44 정합 보강
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.profile
    ADD CONSTRAINT profile_name_unique UNIQUE (name);

-- ─────────────────────────────────────────────────────────
-- 2. user_role.developer_name UNIQUE — V171 line 14 정합 보강
--    developer_name 은 NULL 허용 컬럼이라 partial UNIQUE INDEX 사용
-- ─────────────────────────────────────────────────────────

CREATE UNIQUE INDEX idx_user_role_developer_name_unique
    ON powersales.user_role (developer_name)
    WHERE developer_name IS NOT NULL;
