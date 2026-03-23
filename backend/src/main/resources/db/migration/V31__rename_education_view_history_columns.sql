-- EducationViewHistory 레거시 컬럼명 정리 (#393)
ALTER TABLE education_view_history RENAME COLUMN empcode__c TO emp_code;
ALTER TABLE education_view_history RENAME COLUMN costcentercode__c TO cost_center_code;
