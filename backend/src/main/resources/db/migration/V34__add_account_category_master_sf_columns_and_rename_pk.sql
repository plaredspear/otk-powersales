-- Spec #605 — AccountCategoryMaster SF 어노테이션 정합 + sfid + use_search 컬럼 도입 + PK 컬럼명 정합.
--
-- 단일 권위: Salesforce Object (`AccountCategoryMaster__c`)
-- 정책 (스펙 §2.6 + §6):
--   - PK 컬럼명: id → account_category_master_id (backend-conventions.md `{table_name}_id` 컨벤션)
--   - sfid VARCHAR(18) NULL + partial unique index (Q1 옵션 1)
--   - use_search BOOLEAN NOT NULL DEFAULT false (Q2 옵션 A — SF 레거시 동등, 화이트리스트)

ALTER TABLE powersales.account_category_master
    RENAME COLUMN id TO account_category_master_id;

ALTER TABLE powersales.account_category_master
    ADD COLUMN sfid       varchar(18),
    ADD COLUMN use_search boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX idx_account_category_master_sfid_unique
    ON powersales.account_category_master (sfid)
    WHERE sfid IS NOT NULL;
