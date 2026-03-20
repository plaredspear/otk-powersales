-- Notice 테이블 PK 및 레거시 컬럼명 정리 (#328)
ALTER TABLE salesforce2.notice RENAME COLUMN id TO notice_id;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__scope__c TO scope;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__category__c TO category;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__contents__c TO contents;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__educategory__c TO edu_category;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__jeejum__c TO branch;
ALTER TABLE salesforce2.notice RENAME COLUMN dkretail__jeejumcode__c TO branch_code;
ALTER TABLE salesforce2.notice RENAME COLUMN isdeleted TO is_deleted;
