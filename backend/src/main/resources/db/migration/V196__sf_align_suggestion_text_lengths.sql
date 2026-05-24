-- Suggestion (DKRetail__Proposal__c) 텍스트 컬럼 SF 정합 보강.
--
-- 배경:
--   Stage1 적재 중 car_number 컬럼이 VARCHAR(20) 한계를 초과한 운영 row 로 인해
--   "value too long for type character varying(20)" 발생. SF prod describe 재추출 후
--   다른 컬럼들도 일괄 cross-check 결과 mismatch 6건 발견 — SF 정합 자동 적용 정책에 따라
--   SF length 로 일괄 확장.
--
-- SF prod describe (DKRetail__Proposal__c, retrieved 2026-05-24):
--   CarNumber__c                  string len=255  (entity VARCHAR(20)   → 255)
--   Category__c                   picklist len=255 (entity VARCHAR(50)  → 255)
--   ClaimType__c                  picklist len=255 (entity VARCHAR(200) → 255)
--   ClaimTypeMeasures__c          picklist len=255 (entity VARCHAR(200) → 255)
--   LogisticsResponsibility__c    picklist len=255 (entity VARCHAR(20)  → 255)
--   ProductCode__c                string len=1300  (entity VARCHAR(20)  → 1300)
--
-- 제외 — action_status:
--   ActionStatus__c (picklist len=255) 는 SuggestionActionStatusConverter enum 변환 사용. enum
--   max 한국어값 (예: "조치 완료" = 5자) 기준 30 으로 정의되어 운영 무결성으로 보호. 확장 불요.

ALTER TABLE powersales.suggestion
    ALTER COLUMN car_number              TYPE VARCHAR(255),
    ALTER COLUMN category                TYPE VARCHAR(255),
    ALTER COLUMN claim_type              TYPE VARCHAR(255),
    ALTER COLUMN claim_type_measures     TYPE VARCHAR(255),
    ALTER COLUMN logistics_responsibility TYPE VARCHAR(255),
    ALTER COLUMN product_code            TYPE VARCHAR(1300);
