-- V21: DisplayWorkSchedule 테이블/컬럼명 가독성 개선 (Spec #235)

-- 1. 테이블 리네임
ALTER TABLE salesforce2.displayworkschedulemaster__c
    RENAME TO display_work_schedule;

-- 2. 컬럼 리네임 (11개)
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN account__c TO account;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN fullname__c TO full_name;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN startdate__c TO start_date;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN enddate__c TO end_date;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN confirmed__c TO confirmed;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN typeofwork1__c TO type_of_work1;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN typeofwork3__c TO type_of_work3;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN typeofwork5__c TO type_of_work5;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN createdbyid TO created_by_id;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN ownerid TO owner_id;
ALTER TABLE salesforce2.display_work_schedule RENAME COLUMN createddate TO created_date;

-- 3. 기존 인덱스 삭제
DROP INDEX IF EXISTS salesforce2.hc_idx_displayworkschedulemaster__c_systemmodstamp;
DROP INDEX IF EXISTS salesforce2.hcu_idx_displayworkschedulemaster__c_sfid;

-- 4. 새 인덱스 생성
CREATE INDEX idx_dws_system_mod_stamp ON salesforce2.display_work_schedule (systemmodstamp);
CREATE UNIQUE INDEX uq_dws_sfid ON salesforce2.display_work_schedule (sfid);

-- 5. PK 제약조건 리네임
ALTER TABLE salesforce2.display_work_schedule
    RENAME CONSTRAINT displayworkschedulemaster__c_pkey TO display_work_schedule_pkey;

-- 6. 시퀀스 리네임
ALTER SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq
    RENAME TO display_work_schedule_id_seq;
