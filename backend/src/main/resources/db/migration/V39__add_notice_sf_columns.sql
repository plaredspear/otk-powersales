-- Spec #612 — Notice SF 누락 컬럼 1개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/공지사항(DKRetail__Notice__c).md

ALTER TABLE powersales.notice
    ADD COLUMN title varchar(255);
