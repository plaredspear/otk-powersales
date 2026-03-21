-- SafetyCheckSubmission: master_id(sfid) → display_work_schedule_id(PK) 전환
-- master_id에 저장된 DisplayWorkSchedule sfid를 display_work_schedule.id(PK)로 변환

-- 1. display_work_schedule_id 컬럼 추가
ALTER TABLE salesforce2.safety_check_submission
    ADD COLUMN display_work_schedule_id BIGINT;

-- 2. master_id(sfid) → display_work_schedule.id 매핑
UPDATE salesforce2.safety_check_submission s
SET display_work_schedule_id = d.display_work_schedule_id
FROM salesforce2.display_work_schedule d
WHERE s.master_id = d.sfid
  AND s.master_id IS NOT NULL;
