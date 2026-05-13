-- Spec #753 STEP3 — Claim picklist SF 정합
-- 1. ClaimPurchaseMethod / ClaimRequestType 마스터 테이블 폐지 (SF picklist enum 으로 대체)
-- 2. Claim.purchase_method_code / request_type_code 컬럼 길이 정합 (SF describe 와 일치)

-- 1. 마스터 테이블 drop (active 코드에서 사용 안 함 — 모두 주석 처리 상태)
DROP TABLE IF EXISTS powersales.claim_purchase_methods CASCADE;
DROP TABLE IF EXISTS powersales.claim_request_types CASCADE;

-- 2. 컬럼 길이 정합
-- purchase_method_code: 10 → 255 (SF DKRetail__PurchaseMethod__c length=255)
ALTER TABLE powersales.claim ALTER COLUMN purchase_method_code TYPE VARCHAR(255);

-- request_type_code: 10 → 4099 (SF DKRetail__RequestType__c multipicklist length=4099)
ALTER TABLE powersales.claim ALTER COLUMN request_type_code TYPE VARCHAR(4099);
