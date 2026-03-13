-- V20: TeamMemberSchedule 테이블/컬럼명 가독성 개선 (Spec #231)

-- 1. 테이블 리네임
ALTER TABLE salesforce2.dkretail__teammemberschedule__c
    RENAME TO team_member_schedule;

-- 2. 컬럼 리네임 (34개)
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__employeeid__c TO employee_id;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__workingdate__c TO working_date;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__workingtype__c TO working_type;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__workingcategory1__c TO working_category1;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__workingcategory2__c TO working_category2;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__workingcategory3__c TO working_category3;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN workingcategory4__c TO working_category4;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN accountid__c TO account_id;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN teamleadersfid__c TO team_leader_sfid;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__altholidayid__c TO alt_holiday_id;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__commutelogid__c TO commute_log_id;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__promotionempid__c TO promotion_emp_id;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN dkretail__promotionempidext__c TO promotion_emp_id_ext;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN commutereportdatetime__c TO commute_report_datetime;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN id__c TO id_field;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN traversalflag__c TO traversal_flag;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN isworkreport__c TO is_work_report;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment1__c TO equipment1;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment2__c TO equipment2;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment3__c TO equipment3;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment4__c TO equipment4;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment5__c TO equipment5;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment6__c TO equipment6;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment7__c TO equipment7;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment8__c TO equipment8;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment9__c TO equipment9;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN equipment10__c TO equipment10;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN yes_chkcnt__c TO yes_chk_cnt;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN no_chkcnt__c TO no_chk_cnt;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN precaution_chk__c TO precaution_chk;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN precaution__c TO precaution;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN starttime__c TO start_time;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN completetime__c TO complete_time;
ALTER TABLE salesforce2.team_member_schedule RENAME COLUMN createddate TO created_date;

-- 3. 기존 인덱스 삭제
DROP INDEX IF EXISTS salesforce2.hc_idx_dkretail__teammemberschedule__c_systemmodstamp;
DROP INDEX IF EXISTS salesforce2.hcu_idx_dkretail__teammemberschedule__c_sfid;
DROP INDEX IF EXISTS salesforce2.idx_tms_promotion_emp_id_ext;

-- 4. 새 인덱스 생성
CREATE INDEX idx_tms_system_mod_stamp ON salesforce2.team_member_schedule (systemmodstamp);
CREATE UNIQUE INDEX uq_tms_sfid ON salesforce2.team_member_schedule (sfid);
CREATE UNIQUE INDEX uq_tms_promotion_emp_id_ext ON salesforce2.team_member_schedule (promotion_emp_id_ext);

-- 5. PK 제약조건 리네임
ALTER TABLE salesforce2.team_member_schedule
    RENAME CONSTRAINT dkretail__teammemberschedule__c_pkey TO team_member_schedule_pkey;

-- 6. 시퀀스 리네임
ALTER SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq
    RENAME TO team_member_schedule_id_seq;
