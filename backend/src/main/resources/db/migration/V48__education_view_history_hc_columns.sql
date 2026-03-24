-- edu_id: Heroku community_id 원본 값 저장용
ALTER TABLE education_view_history ADD COLUMN edu_id VARCHAR(20);

-- emp_code: Heroku empcode__c 원본 값 저장용
ALTER TABLE education_view_history ADD COLUMN emp_code VARCHAR(40);

-- FK NOT NULL 해제 (마이그레이션 INSERT 시 NULL 허용)
ALTER TABLE education_view_history ALTER COLUMN education_post_id DROP NOT NULL;
ALTER TABLE education_view_history ALTER COLUMN employee_id DROP NOT NULL;
