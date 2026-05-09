-- Spec #609 — TeamMemberSchedule SF 누락 컬럼 7개 신규 도입 (Q1 옵션 1).
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/여사원일정(DKRetail__TeamMemberSchedule__c).md
-- 정책 (스펙 §6.3 + Q2 옵션 1):
--   - DB 컬럼명: SF 한국어 라벨 의미 + snake_case 가독성 우선
--   - Lookup 필드는 <관계명>_sfid (긴 이름 — entity 명 일관성 우선)

ALTER TABLE powersales.team_member_schedule
    ADD COLUMN hr_code                                          varchar(40),
    ADD COLUMN promotion_emp_id_ext                             varchar(40),
    ADD COLUMN second_work_type                                 varchar(40),
    ADD COLUMN working_category5                                varchar(40),
    ADD COLUMN ref_account_name                                 varchar(255),
    ADD COLUMN monthly_female_employee_integration_schedule_sfid varchar(18),
    ADD COLUMN professional_promotion_team                      varchar(100);
