-- Spec #618 — Organization SF 누락 비수식 1개 신규 도입 (Q1 옵션 2: UNIQUE 미부착).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/조직(Org__c).md
--
-- 구현 결정:
--   - external_key: VARCHAR(100) NULL — SF ExternalKey__c 텍스트(100) 정합
--   - UNIQUE 제약 미부착 (Q1 옵션 2 — 정합 검증 후속). SF 측은 외부 ID 고유.

ALTER TABLE powersales.organization
    ADD COLUMN external_key varchar(100);
