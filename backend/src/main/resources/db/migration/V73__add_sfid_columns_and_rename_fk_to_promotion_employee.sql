-- promotion_employee: sfid 컬럼 추가 + schedule_id → team_member_schedule_id 리네이밍

ALTER TABLE promotion_employee ADD COLUMN promotion_sfid VARCHAR(18);
ALTER TABLE promotion_employee ADD COLUMN team_member_schedule_sfid VARCHAR(18);
ALTER TABLE promotion_employee RENAME COLUMN schedule_id TO team_member_schedule_id;
