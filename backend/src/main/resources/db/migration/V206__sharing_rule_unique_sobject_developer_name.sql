-- sharing_rule 의 자연 unique 키를 (developer_name) 단일에서 (s_object_name, developer_name) 복합으로 전환.
--
-- 직전 V184 (실효 idx_sharing_rule_developer_name_unique) 는 developer_name 단일 컬럼 UNIQUE 였으나,
-- SF SharingRule 의 진짜 자연 키는 (sObjectName, developerName) 두 컬럼이다 (V205 의 condition/target
-- 보강 이유와 동종 사고).
--
-- 운영 영향 (V206 미적용 시):
--   - 한 SF retrieve 안에서 같은 fullName 이 여러 sObject 의 sharingRules-meta.xml 에 동시 정의되면
--     (예: X5452 가 Account / DisplayWorkScheduleMaster__c / MonthlyFemaleEmployeeIntegrationSchedule__c
--     3개 sObject 동시 정의), Stage1 적재 시 2번째 이후 sObject row 가 ON CONFLICT DO NOTHING 으로 drop.
--   - 운영 dev 실측: CSV 335 row → DB 293 row (42 row 누락) → sharing_rule_condition / target 의
--     sharing_rule_id 채움 시 ref row 부재로 88 row (condition 46 + target 42) NULL 잔존.
--
-- 본 마이그레이션:
--   1. 기존 단일 UNIQUE drop
--   2. (s_object_name, developer_name) 복합 UNIQUE 생성
--
-- 후속 코드 변경:
--   - SharingRule entity: @Column unique 제거 + class-level uniqueConstraints
--   - Stage1Targets.SHARING_RULE: preClear = true (V206 적용 후 1회 전량 재적재 필요)

DROP INDEX IF EXISTS powersales.idx_sharing_rule_developer_name_unique;

CREATE UNIQUE INDEX idx_sharing_rule_s_object_developer_name_unique
    ON powersales.sharing_rule (s_object_name, developer_name);
