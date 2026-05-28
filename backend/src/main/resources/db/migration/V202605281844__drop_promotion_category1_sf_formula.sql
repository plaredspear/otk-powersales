-- SF Formula 필드 정합 — Promotion.category1 컬럼 제거
-- Spec #740 Track A 후속: V96 에서 누락되었던 Category1__c 추가 정리.
-- SF `DKRetail__Promotion__c.Category1__c` 는 formula (`DKRetail__PrimaryProductId__r.StoreCondition__c`) 로
-- read-only 파생 값. entity 측은 derived property (primaryProduct.storeConditionText) 로 전환.
ALTER TABLE promotion DROP COLUMN IF EXISTS category1;
