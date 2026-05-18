-- SF DKRetail__Claim__c.DKRetail__ReasonType__c (string 100) 정합
-- 기존 reason_type VARCHAR(20) → VARCHAR(100) — 절단 위험 회피
ALTER TABLE claim ALTER COLUMN reason_type TYPE VARCHAR(100);
