-- Promotion.category1 컬럼 추가 (SF Promotion.Category1__c, string length=1300, label="제품유형")
-- 레거시 PromotionEmployeeTriggerHandler 의 대표제품 vs 전문행사조 매칭 검증 입력값
-- 운영 데이터: "라면" / "냉장" / "냉동" / "만두" 등 자유 문자열

ALTER TABLE promotion ADD COLUMN category1 VARCHAR(1300);
