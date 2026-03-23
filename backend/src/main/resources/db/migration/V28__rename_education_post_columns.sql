-- EducationPost 컬럼명 가독성 개선 (#390)
ALTER TABLE salesforce2.education_post RENAME COLUMN edu_id TO education_post_id;
ALTER TABLE salesforce2.education_post RENAME COLUMN edu_title TO title;
ALTER TABLE salesforce2.education_post RENAME COLUMN edu_content TO content;
ALTER TABLE salesforce2.education_post RENAME COLUMN edu_code TO education_code;
ALTER TABLE salesforce2.education_post RENAME COLUMN empcode__c TO emp_code;
