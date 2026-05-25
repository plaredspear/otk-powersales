-- sharing_rule_condition / sharing_rule_target 자연 키에 sObjectName 보강.
--
-- SF SharingRule 의 자연 키는 (sObjectName, developerName) 두 컬럼.
-- 한 retrieve 산출물 안에서 같은 developerName 이 여러 sObject 의 sharingRules-meta.xml
-- 에 동시에 정의될 수 있다 (예: X5452 가 Account / DisplayWorkScheduleMaster__c /
-- MonthlyFemaleEmployeeIntegrationSchedule__c 3개 sObject 의 sharingRules 에 동시 정의).
--
-- V184 에서 추가한 sharing_rule_developer_name 단일 컬럼만으로는 Stage2 fk-natural-key
-- resolve 시 sharing_rule.developer_name 만 lookup 하므로 6 sharing_rule row 중 첫 번째
-- (sharing_rule_id 가장 작은 것) 에만 매칭 → 나머지 5 row 의 condition 들이 모두 같은
-- (sharing_rule_id, condition_order) 로 채워져 unique index idx_sharing_rule_condition_rule_order_unique 위반.
--
-- 본 마이그레이션은 sharing_rule_s_object_name 컬럼을 추가해 (s_object_name, developer_name)
-- 복합 자연 키로 정확한 sharing_rule row 와 매칭되도록 한다.
--
-- 후속 코드 변경:
--   - extract-sharing-meta.main.kts: sharing-rule-condition.csv / sharing-rule-target.csv
--     헤더에 sObjectName 컬럼 추가
--   - SharingRuleCondition / SharingRuleTarget entity: sharingRuleSObjectName 필드 추가
--   - Stage1Targets: 두 entity 매핑에 sObjectName 필드 추가
--   - SfMigrationStage2NaturalKeyFkService: 두 테이블 전용 resolve method
--     (NATURAL_KEY_FK_MAPPINGS 단일 컬럼 spec 으로 표현 불가)

-- ─────────────────────────────────────────────────────────
-- 1. sharing_rule_condition.sharing_rule_s_object_name
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.sharing_rule_condition
    ADD COLUMN sharing_rule_s_object_name VARCHAR(80);

COMMENT ON COLUMN powersales.sharing_rule_condition.sharing_rule_s_object_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 (s_object_name, developer_name) → sharing_rule.sharing_rule_id 채움';

-- ─────────────────────────────────────────────────────────
-- 2. sharing_rule_target.sharing_rule_s_object_name
-- ─────────────────────────────────────────────────────────

ALTER TABLE powersales.sharing_rule_target
    ADD COLUMN sharing_rule_s_object_name VARCHAR(80);

COMMENT ON COLUMN powersales.sharing_rule_target.sharing_rule_s_object_name IS
    'XML 메타 출처 자연 키 — Stage 2 fk resolve 가 (s_object_name, developer_name) → sharing_rule.sharing_rule_id 채움';
