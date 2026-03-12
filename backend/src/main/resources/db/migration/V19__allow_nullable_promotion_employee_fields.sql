-- 행사사원 빈 레코드 생성 허용 (스펙 #218)
-- 등록 시 검증 없이 즉시 INSERT하므로 필수 필드를 nullable로 변경

ALTER TABLE promotion_employee ALTER COLUMN employee_sfid DROP NOT NULL;
ALTER TABLE promotion_employee ALTER COLUMN schedule_date DROP NOT NULL;
ALTER TABLE promotion_employee ALTER COLUMN work_status DROP NOT NULL;
ALTER TABLE promotion_employee ALTER COLUMN work_type1 DROP NOT NULL;
ALTER TABLE promotion_employee ALTER COLUMN work_type3 DROP NOT NULL;
