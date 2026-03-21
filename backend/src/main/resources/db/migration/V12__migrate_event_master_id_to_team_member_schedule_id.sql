-- SafetyCheckSubmission eventMasterId(sfid) → teamMemberScheduleId(PK) 전환 (#366)

-- Step 1: team_member_schedule_id 컬럼 추가
ALTER TABLE safety_check_submission ADD COLUMN team_member_schedule_id BIGINT;

-- Step 2: event_master_id(sfid) 기반으로 team_member_schedule.id 매핑
UPDATE safety_check_submission s
SET team_member_schedule_id = tms.id
FROM team_member_schedule tms
WHERE s.event_master_id IS NOT NULL
  AND s.event_master_id <> ''
  AND s.event_master_id = tms.sfid;
