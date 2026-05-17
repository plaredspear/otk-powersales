-- PromotionEmployee 의 owner 컬럼군 제거.
--
-- 배경:
--   SF prod 메타 (DKRetail__PromotionEmployee__c) 에 OwnerId 필드 부재 — sobject ownable=false,
--   master-detail child (부모 Promotion 의 owner 따름). V85 에서 owner_sfid / owner_id / FK / index
--   를 일괄 추가하였으나 SF 측 source 데이터가 존재하지 않아 마이그레이션 / 운영 양 측면에서 의미 없음.
--   V133 주석에서도 본 사실을 이미 명시하고 "보존" 으로 결정했으나, 본 마이그레이션으로 폐기 전환.
--
-- 영향:
--   - backend 코드의 PromotionEmployee.ownerSfid / owner 사용처 0건 (사전 grep 검증).
--   - SF cut-over ETL (Spec #764) 의 PROMOTION_EMPLOYEE_METADATA / SOQL 에서 OwnerId 제외 정합.

BEGIN;

DROP INDEX IF EXISTS powersales.idx_promotion_employee_owner_id;

ALTER TABLE powersales.promotion_employee
    DROP CONSTRAINT IF EXISTS fk_promotion_employee_owner;

ALTER TABLE powersales.promotion_employee
    DROP COLUMN IF EXISTS owner_sfid,
    DROP COLUMN IF EXISTS owner_id;

COMMIT;
