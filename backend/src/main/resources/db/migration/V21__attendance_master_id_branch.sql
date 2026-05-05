-- Spec #587 P1-B: 출근등록 진열 마스터 분기 + AttendanceType enum 도입
--
-- 1. team_member_schedule.display_work_schedule_sfid + display_work_schedule_id 페어 추가
--    - SF 표준 모델에서 진열 마스터 연결 위치는 DKRetail__TeamMemberSchedule__c.DisplayWorkScheduleMaster__c
--    - HC 컬럼명: displayworkschedulemaster__c
--    - FK 제약은 본 스펙 비범위 (spec.md §6.2 박스) — 인덱스만 부여
--
-- 2. attendance_log.attendance_type 컬럼 추가
--    - VARCHAR(20) NOT NULL DEFAULT 'REGULAR'
--    - DB 체크 제약 미부여 — 애플리케이션 AttendanceType enum 으로만 검증 (spec.md §3.2)

ALTER TABLE powersales.team_member_schedule
    ADD COLUMN IF NOT EXISTS display_work_schedule_sfid VARCHAR(18) NULL,
    ADD COLUMN IF NOT EXISTS display_work_schedule_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_team_member_schedule_display_work_schedule_id
    ON powersales.team_member_schedule (display_work_schedule_id);

ALTER TABLE powersales.attendance_log
    ADD COLUMN IF NOT EXISTS attendance_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR';
