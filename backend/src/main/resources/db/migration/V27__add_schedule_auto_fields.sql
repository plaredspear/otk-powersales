-- 진열스케줄 업로드 자동 설정 필드 추가 (#274)
ALTER TABLE display_work_schedule ADD COLUMN cost_center_code VARCHAR(20);
ALTER TABLE display_work_schedule ADD COLUMN last_month_revenue BIGINT;
