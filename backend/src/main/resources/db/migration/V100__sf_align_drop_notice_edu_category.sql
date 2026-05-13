-- Spec #745 STEP1 / Q2: Notice.edu_category (DKRetail__EduCategory__c, Label="사용안함") 컬럼 제거.
-- 근거: README §6.2 E분류(사용안함) 자동 제외 정책.

ALTER TABLE notice DROP COLUMN IF EXISTS edu_category;
