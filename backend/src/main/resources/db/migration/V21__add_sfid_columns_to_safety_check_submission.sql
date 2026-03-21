-- safety_check_submission 테이블에 sfid 컬럼 추가 (Heroku 레거시 SFID 원본 값 저장용)
ALTER TABLE safety_check_submission ADD COLUMN employee_sfid VARCHAR(18);
ALTER TABLE safety_check_submission ADD COLUMN display_work_schedule_sfid VARCHAR(18);
ALTER TABLE safety_check_submission ADD COLUMN team_member_schedule_sfid VARCHAR(18);
