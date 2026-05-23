-- SharingRuleCondition / SharingRuleTarget Stage1 정규 적재용 스키마 보강.
--
-- 변경 사항:
--   1. sharing_rule_condition.sharing_rule_developer_name VARCHAR(80) 신규 컬럼
--      + sharing_rule_id NOT NULL 제약 해제 (Stage1 적재 시 NULL, Stage2 fk substep 후 채움)
--   2. sharing_rule_target.sharing_rule_developer_name VARCHAR(80) 신규 컬럼
--      + sharing_rule_id NOT NULL 제약 해제 (Stage1 적재 시 NULL, Stage2 fk substep 후 채움)
--
-- Stage 2 fk substep 동작:
--   sharing_rule_condition.sharing_rule_developer_name → sharing_rule.developer_name lookup
--   → sharing_rule_id 채움 (sharing_rule_target 동일)
--
-- 본 마이그레이션은 V175 의 schema 를 ALTER 로 보완. V175 본문은 수정 금지 (CLAUDE.md 정책).

-- ─────────────────────────────────────────────────────────
-- 1. sharing_rule_condition — sharing_rule_developer_name + sharing_rule_id NOT NULL 해제
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.sharing_rule_condition
    ALTER COLUMN sharing_rule_id DROP NOT NULL;

ALTER TABLE powersales.sharing_rule_condition
    ADD COLUMN sharing_rule_developer_name VARCHAR(80);

COMMENT ON COLUMN powersales.sharing_rule_condition.sharing_rule_developer_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 sharing_rule.developer_name → sharing_rule_id 채움';

-- ─────────────────────────────────────────────────────────
-- 2. sharing_rule_target — sharing_rule_developer_name + sharing_rule_id NOT NULL 해제
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.sharing_rule_target
    ALTER COLUMN sharing_rule_id DROP NOT NULL;

ALTER TABLE powersales.sharing_rule_target
    ADD COLUMN sharing_rule_developer_name VARCHAR(80);

COMMENT ON COLUMN powersales.sharing_rule_target.sharing_rule_developer_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 sharing_rule.developer_name → sharing_rule_id 채움';
