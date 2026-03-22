-- Employee 테이블에 발령 상세 필드 10개 추가 (Spec #387)
ALTER TABLE employee ADD COLUMN jikchak VARCHAR(100);
ALTER TABLE employee ADD COLUMN jikwee VARCHAR(40);
ALTER TABLE employee ADD COLUMN jikgub VARCHAR(40);
ALTER TABLE employee ADD COLUMN work_type VARCHAR(40);
ALTER TABLE employee ADD COLUMN job_code VARCHAR(40);
ALTER TABLE employee ADD COLUMN work_area VARCHAR(100);
ALTER TABLE employee ADD COLUMN jikjong VARCHAR(40);
ALTER TABLE employee ADD COLUMN appointment_date DATE;
ALTER TABLE employee ADD COLUMN ord_detail_node VARCHAR(255);
ALTER TABLE employee ADD COLUMN crm_work_start_date DATE;
