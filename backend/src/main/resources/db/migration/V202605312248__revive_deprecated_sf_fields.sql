-- V202605312248: Spec #849 — SF 메타 존재 non-calculated deprecated 필드 4건 마이그레이션 대상 부활.
--
-- 원칙: SF 메타에 존재하는 (non-calculated) 필드는 데이터 존재 여부 무관하게 모두 마이그레이션 대상.
-- 과거 "사용안함 자동 제외" 정책으로 미도입/제거(V100 DROP)된 필드를 부활.
--
--  #1 team_member_schedule.DKRetail__AccountId__c → dk_account_sfid + dk_account_id (FK→account)
--  #3 promotion.DKRetail__AccId__c              → dk_account_sfid + dk_account_id (FK→account)
--  #2 notice.DKRetail__EduCategory__c           → edu_category (plain varchar, V100 DROP 부활)
--  #4 suggestion.DKRetail__Category__c          → dk_category (plain Text 40)
--
-- FK 제약은 기존 account_id 패턴(V1 promotion_account_id_fkey / team_member_schedule_account_id_fkey
-- = plain FOREIGN KEY ... REFERENCES powersales.account(account_id), ON DELETE 미지정)과 정합.
-- 모든 컬럼 nullable.

-- #1 team_member_schedule
ALTER TABLE powersales.team_member_schedule ADD COLUMN dk_account_sfid VARCHAR(18);
ALTER TABLE powersales.team_member_schedule ADD COLUMN dk_account_id BIGINT;
ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT team_member_schedule_dk_account_id_fkey FOREIGN KEY (dk_account_id) REFERENCES powersales.account(account_id);

-- #3 promotion
ALTER TABLE powersales.promotion ADD COLUMN dk_account_sfid VARCHAR(18);
ALTER TABLE powersales.promotion ADD COLUMN dk_account_id BIGINT;
ALTER TABLE powersales.promotion
    ADD CONSTRAINT promotion_dk_account_id_fkey FOREIGN KEY (dk_account_id) REFERENCES powersales.account(account_id);

-- #2 notice (V100 DROP 부활)
ALTER TABLE powersales.notice ADD COLUMN edu_category VARCHAR(255);

-- #4 suggestion
ALTER TABLE powersales.suggestion ADD COLUMN dk_category VARCHAR(40);
