-- V29: Account sfid 참조를 Account.id(PK)로 전환 (Spec #277)

-- 1. TeamMemberSchedule: account_id (VARCHAR sfid → INTEGER id)
ALTER TABLE team_member_schedule ADD COLUMN account_id_new INTEGER;

UPDATE team_member_schedule tms
SET account_id_new = a.id
FROM account a
WHERE tms.account_id = a.sfid;

ALTER TABLE team_member_schedule DROP COLUMN account_id;
ALTER TABLE team_member_schedule RENAME COLUMN account_id_new TO account_id;

-- 2. DisplayWorkSchedule: account (VARCHAR sfid → INTEGER account_id)
ALTER TABLE display_work_schedule ADD COLUMN account_id INTEGER;

UPDATE display_work_schedule dws
SET account_id = a.id
FROM account a
WHERE dws.account = a.sfid;

ALTER TABLE display_work_schedule DROP COLUMN account;
