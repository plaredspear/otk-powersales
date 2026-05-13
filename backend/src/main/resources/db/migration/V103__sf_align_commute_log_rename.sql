-- Spec #749 — TeamMemberSchedule.commute_log_id → commute_log_sfid 리네임 + AttendanceLog FK 신설.
-- 근거: README §6.4 R-2 + §6.8 컬럼명 컨벤션.

-- 1. sfid buffer 컬럼 리네임 (`_id` → `_sfid`)
ALTER TABLE team_member_schedule RENAME COLUMN commute_log_id TO commute_log_sfid;

-- 2. AttendanceLog FK 신설
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS attendance_log_id BIGINT
    REFERENCES attendance_log(attendance_log_id);
CREATE INDEX IF NOT EXISTS idx_tms_attendance_log_id ON team_member_schedule(attendance_log_id);
