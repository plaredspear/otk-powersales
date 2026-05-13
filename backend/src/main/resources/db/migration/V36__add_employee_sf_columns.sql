-- Spec #607 — Employee SF 누락 컬럼 8개 신규 도입.
--
-- 단일 권위: Salesforce Object (`DKRetail__Employee__c`)
-- 정책 (스펙 §6.3):
--   - dk_cost_center_code: 기존 cost_center_code(조직유형) 와 별개 (Q2 옵션 1 — DK_ prefix 회피 명명)
--   - Lookup 필드는 <관계명>_sfid (manager_sfid, postponed_appointment_sfid)
--   - 숫자(18,0) 는 NUMERIC(18,0)
--   - Boolean 은 BOOLEAN

ALTER TABLE powersales.employee
    ADD COLUMN dk_cost_center_code        varchar(3),
    ADD COLUMN location_code              varchar(100),
    ADD COLUMN total_annual_leave         numeric(18, 0),
    ADD COLUMN used_annual_leave          numeric(18, 0),
    ADD COLUMN manager_sfid               varchar(18),
    ADD COLUMN postponed_appointment_sfid varchar(18),
    ADD COLUMN locking_flag               boolean,
    ADD COLUMN prn_flag                   varchar(100);
