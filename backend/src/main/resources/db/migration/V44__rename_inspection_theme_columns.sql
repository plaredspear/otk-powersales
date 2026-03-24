-- inspection_theme: PK 및 레거시 컬럼명 가독성 개선 (#410)
ALTER TABLE inspection_theme RENAME COLUMN id TO inspection_theme_id;
ALTER TABLE inspection_theme RENAME COLUMN title__c TO title;
ALTER TABLE inspection_theme RENAME COLUMN startdate__c TO start_date;
ALTER TABLE inspection_theme RENAME COLUMN enddate__c TO end_date;
ALTER TABLE inspection_theme RENAME COLUMN department__c TO department;
ALTER TABLE inspection_theme RENAME COLUMN branchcode__c TO branch_code;
ALTER TABLE inspection_theme RENAME COLUMN publicflag__c TO public_flag;
ALTER TABLE inspection_theme RENAME COLUMN isdeleted TO is_deleted;
