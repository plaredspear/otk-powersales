-- Spec #329: DisplayWorkSchedule PK 컬럼명 및 isdeleted 컬럼명 컨벤션 변경
ALTER TABLE display_work_schedule RENAME COLUMN id TO display_work_schedule_id;
ALTER TABLE display_work_schedule RENAME COLUMN isdeleted TO is_deleted;
