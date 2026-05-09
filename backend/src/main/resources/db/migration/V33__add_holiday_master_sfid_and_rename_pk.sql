-- Spec #604 — HolidayMaster SF 어노테이션 정합 + sfid 컬럼 도입 + PK 컬럼명 정합.
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/휴무일마스터(HolidayMaster__c).md
-- 정책 (스펙 §2.6 + §6):
--   - PK 컬럼명: id → holiday_master_id (backend-conventions.md `{table_name}_id` 컨벤션)
--   - sfid VARCHAR(18) 신규, UNIQUE 제약 없음 (Q2 옵션 2)
--   - 운영 데이터 백필 SQL 미포함 (Q3 — 런칭 전 데이터 부재)

ALTER TABLE powersales.holiday_master
    RENAME COLUMN id TO holiday_master_id;

ALTER TABLE powersales.holiday_master
    ADD COLUMN sfid varchar(18);
