-- Spec #617 — StaffReview SF 누락 비수식 5개 신규 도입 (Q1 옵션 1, Q2 옵션 1).
--
-- 단일 권위: Salesforce Object (`StaffReview__c`)
--
-- 구현 결정:
--   - working_category1/2/3: VARCHAR(255) — SF picklist + 기존 entity 컨벤션(team_member_schedule.working_category1/2/3) 정합 (스펙 §6.2 추정 40 → 절단 방지)
--   - job_code: VARCHAR(20) — SF JobCode__c 텍스트(20) + 기존 entity 컨벤션(appointment.job_code) 정합 (스펙 §6.2 추정 40 → SF 정합 20)
--   - first_day_of_month: DATE — SF FirstDayofMonth__c 날짜 정합

ALTER TABLE powersales.staff_review
    ADD COLUMN working_category1  varchar(255),
    ADD COLUMN working_category2  varchar(255),
    ADD COLUMN working_category3  varchar(255),
    ADD COLUMN job_code           varchar(20),
    ADD COLUMN first_day_of_month date;
