-- V1: Legacy PowerSales schema migration
-- Source: docs/plan/기존소스/파워세일즈 스키마.SQL (2,652 lines)
-- Legacy Heroku Connect user (u4bee3ek26k44g) references removed.
-- Schema objects are owned by the Flyway execution user (= datasource user).
-- HC infrastructure (triggers, functions) removed — no longer needed without Heroku Connect.

CREATE EXTENSION IF NOT EXISTS hstore;

-- DROP SCHEMA salesforce2;

CREATE SCHEMA IF NOT EXISTS salesforce2;

-- DROP SEQUENCE salesforce2._hcmeta_id_seq;

CREATE SEQUENCE salesforce2._hcmeta_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2._sf_event_log_id_seq;

CREATE SEQUENCE salesforce2._sf_event_log_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2._trigger_log_id_seq;

CREATE SEQUENCE salesforce2._trigger_log_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.account_id_seq;

CREATE SEQUENCE salesforce2.account_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.agreementword__c_id_seq;

CREATE SEQUENCE salesforce2.agreementword__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq;

CREATE SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.dkretail__employee__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__employee__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.dkretail__notice__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__notice__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.dkretail__product__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__product__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.expirationdate__mng_id_seq;

CREATE SEQUENCE salesforce2.expirationdate__mng_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.hqreview__c_id_seq;

CREATE SEQUENCE salesforce2.hqreview__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.monthlysaleshistory__c_id_seq;

CREATE SEQUENCE salesforce2.monthlysaleshistory__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.productbarcode__c_id_seq;

CREATE SEQUENCE salesforce2.productbarcode__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.pushmessage__c_id_seq;

CREATE SEQUENCE salesforce2.pushmessage__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.pushmessagereceiver__c_id_seq;

CREATE SEQUENCE salesforce2.pushmessagereceiver__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.staffreview__c_id_seq;

CREATE SEQUENCE salesforce2.staffreview__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.theme__c_id_seq;

CREATE SEQUENCE salesforce2.theme__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- DROP SEQUENCE salesforce2.uploadfile__c_id_seq;

CREATE SEQUENCE salesforce2.uploadfile__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- salesforce2._hcmeta definition

-- Drop table

-- DROP TABLE salesforce2._hcmeta;

CREATE TABLE salesforce2._hcmeta ( id serial4 NOT NULL, hcver int4 NULL, org_id varchar(50) NULL, details text NULL, CONSTRAINT _hcmeta_pkey PRIMARY KEY (id));

-- salesforce2._sf_event_log definition

-- Drop table

-- DROP TABLE salesforce2._sf_event_log;

CREATE TABLE salesforce2._sf_event_log ( id serial4 NOT NULL, table_name varchar(128) NULL, "action" varchar(7) NULL, synced_at timestamptz DEFAULT now() NULL, sf_timestamp timestamptz NULL, sfid varchar(20) NULL, record text NULL, processed bool NULL, CONSTRAINT _sf_event_log_pkey PRIMARY KEY (id));
CREATE INDEX idx__sf_event_log_comp_key ON salesforce2._sf_event_log USING btree (table_name, synced_at);
CREATE INDEX idx__sf_event_log_sfid ON salesforce2._sf_event_log USING btree (sfid);

-- salesforce2._trigger_log definition

-- Drop table

-- DROP TABLE salesforce2._trigger_log;

CREATE TABLE salesforce2._trigger_log ( id serial4 NOT NULL, txid int8 NULL, created_at timestamptz DEFAULT now() NULL, updated_at timestamptz DEFAULT now() NULL, processed_at timestamptz NULL, processed_tx int8 NULL, state varchar(8) NULL, "action" varchar(7) NULL, table_name varchar(128) NULL, record_id int4 NULL, sfid varchar(18) NULL, "old" text NULL, "values" text NULL, sf_result int4 NULL, sf_message text NULL, CONSTRAINT _trigger_log_pkey PRIMARY KEY (id));
CREATE INDEX _trigger_log_idx_created_at ON salesforce2._trigger_log USING btree (created_at);
CREATE INDEX _trigger_log_idx_state_id ON salesforce2._trigger_log USING btree (state, id);
CREATE INDEX _trigger_log_idx_state_table_name ON salesforce2._trigger_log USING btree (state, table_name) WHERE (((state)::text = 'NEW'::text) OR ((state)::text = 'PENDING'::text));

-- salesforce2._trigger_log_archive definition

-- Drop table

-- DROP TABLE salesforce2._trigger_log_archive;

CREATE TABLE salesforce2._trigger_log_archive ( id int4 NOT NULL, txid int8 NULL, created_at timestamptz NULL, updated_at timestamptz NULL, processed_at timestamptz NULL, processed_tx int8 NULL, state varchar(8) NULL, "action" varchar(7) NULL, table_name varchar(128) NULL, record_id int4 NULL, sfid varchar(18) NULL, "old" text NULL, "values" text NULL, sf_result int4 NULL, sf_message text NULL, CONSTRAINT _trigger_log_archive_pkey PRIMARY KEY (id));
CREATE INDEX _trigger_log_archive_idx_created_at ON salesforce2._trigger_log_archive USING btree (created_at);
CREATE INDEX _trigger_log_archive_idx_record_id ON salesforce2._trigger_log_archive USING btree (record_id);
CREATE INDEX _trigger_log_archive_idx_state_table_name ON salesforce2._trigger_log_archive USING btree (state, table_name) WHERE ((state)::text = 'FAILED'::text);

-- salesforce2.account definition

-- Drop table

-- DROP TABLE salesforce2.account;

CREATE TABLE salesforce2.account ( address2__c varchar(120) NULL, address1__c varchar(120) NULL, representative__c varchar(100) NULL, abctype__c varchar(20) NULL, "name" varchar(255) NULL, phone varchar(40) NULL, externalkey__c varchar(100) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, accountgroup__c varchar(10) NULL, closingtime3__c varchar(50) NULL, closingtime2__c varchar(50) NULL, longitude__c varchar(100) NULL, closingtime1__c varchar(50) NULL, branchcode__c varchar(100) NULL, zipcode__c varchar(100) NULL, branchname__c varchar(250) NULL, createddate timestamp NULL, latitude__c varchar(100) NULL, mobilephone__c varchar(40) NULL, abctypecode__c varchar(40) NULL, industry varchar(255) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, werk1_tx__c varchar(255) NULL, werk2_tx__c varchar(255) NULL, werk3_tx__c varchar(255) NULL, CONSTRAINT account_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_account_systemmodstamp ON salesforce2.account USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_account_sfid ON salesforce2.account USING btree (sfid);

-- salesforce2.agreementword__c definition

-- Drop table

-- DROP TABLE salesforce2.agreementword__c;

CREATE TABLE salesforce2.agreementword__c ( contents__c varchar(8000) NULL, "name" varchar(80) NULL, activedate__c date NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, afteractivedate__c date NULL, createddate timestamp NULL, active__c bool NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT agreementword__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_agreementword__c_systemmodstamp ON salesforce2.agreementword__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_agreementword__c_sfid ON salesforce2.agreementword__c USING btree (sfid);

-- salesforce2.commute_distance definition

-- Drop table

-- DROP TABLE salesforce2.commute_distance;

CREATE TABLE salesforce2.commute_distance ( distance int4 NULL);
COMMENT ON TABLE salesforce2.commute_distance IS '출근거리';

-- salesforce2.device_version_mng definition

-- Drop table

-- DROP TABLE salesforce2.device_version_mng;

CREATE TABLE salesforce2.device_version_mng ( "version" varchar(10) NOT NULL, device varchar(10) NOT NULL, createdate timestamp NOT NULL, contents varchar(1000) NOT NULL, s3_key varchar(200) NOT NULL, file_url varchar(300) NULL, s3_key_ipa varchar(200) NULL, file_url_ipa varchar(300) NULL, CONSTRAINT device_version_mng_pk PRIMARY KEY (version, device));

-- salesforce2.displayworkschedulemaster__c definition

-- Drop table

-- DROP TABLE salesforce2.displayworkschedulemaster__c;

CREATE TABLE salesforce2.displayworkschedulemaster__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, account__c varchar(18) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, confirmed__c bool NULL, fullname__c varchar(18) NULL, typeofwork5__c varchar(255) NULL, typeofwork3__c varchar(255) NULL, startdate__c date NULL, typeofwork1__c varchar(255) NULL, enddate__c date NULL, createdbyid varchar(18) NULL, ownerid varchar(18) NULL, CONSTRAINT displayworkschedulemaster__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_displayworkschedulemaster__c_systemmodstamp ON salesforce2.displayworkschedulemaster__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_displayworkschedulemaster__c_sfid ON salesforce2.displayworkschedulemaster__c USING btree (sfid);

-- salesforce2.dkretail__employee__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__employee__c;

CREATE TABLE salesforce2.dkretail__employee__c ( dkretail__birthdate__c varchar(10) NULL, dkretail__status__c varchar(40) NULL, dkretail__apploginactive__c bool NULL, "name" varchar(80) NULL, agreementflag__c bool NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__workphone__c varchar(255) NULL, costcentercode__c varchar(10) NULL, phone__c varchar(40) NULL, createddate timestamp NULL, dkretail__appauthority__c varchar(255) NULL, dkretail__empcode__c varchar(100) NULL, dkretail__startdate__c date NULL, dkretail__homephone__c varchar(255) NULL, dkretail__orgname__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT dkretail__employee__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__employee__c_systemmodstamp ON salesforce2.dkretail__employee__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__employee__c_sfid ON salesforce2.dkretail__employee__c USING btree (sfid);

-- salesforce2.dkretail__notice__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__notice__c;

CREATE TABLE salesforce2.dkretail__notice__c ( dkretail__jeejum__c varchar(255) NULL, "name" varchar(80) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__jeejumcode__c varchar(255) NULL, createddate timestamp NULL, dkretail__scope__c varchar(255) NULL, dkretail__category__c varchar(255) NULL, dkretail__contents__c text NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, dkretail__educategory__c varchar(255) NULL, employeeid__c varchar(18) NULL, CONSTRAINT dkretail__notice__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__notice__c_employeeid__c ON salesforce2.dkretail__notice__c USING btree (employeeid__c);
CREATE INDEX hc_idx_dkretail__notice__c_systemmodstamp ON salesforce2.dkretail__notice__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__notice__c_sfid ON salesforce2.dkretail__notice__c USING btree (sfid);

-- salesforce2.dkretail__product__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__product__c;

CREATE TABLE salesforce2.dkretail__product__c ( dkretail__orderingunit__c varchar(40) NULL, dkretail__shelflifeunit__c varchar(40) NULL, dkretail__producttype__c varchar(255) NULL, dkretail__standardunitprice__c float8 NULL, dkretail__categorycode3__c varchar(100) NULL, dkretail__categorycode2__c varchar(100) NULL, dkretail__categorycode1__c varchar(100) NULL, "name" varchar(80) NULL, dkretail__productstatus__c varchar(255) NULL, shelflifefull__c varchar(1300) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__productcode__c varchar(100) NULL, supertax__c float8 NULL, dkretail__unit__c varchar(40) NULL, createddate timestamp NULL, standardprice__c float8 NULL, dkretail__category3__c varchar(255) NULL, dkretail__launchdate__c date NULL, dkretail__conversionquantity__c float8 NULL, dkretail__boxreceivingquantity__c float8 NULL, dkretail__category2__c varchar(255) NULL, dkretail__category1__c varchar(255) NULL, tastegift__c varchar(1) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, dkretail__shelflife__c varchar(30) NULL, dkretail__storecondition__c varchar(255) NULL, productfeatures__c varchar(255) NULL, imgrefpath_front__c varchar(255) NULL, imgrefpathtxt__c varchar(255) NULL, dkretail__logisticsbarcode__c varchar(100) NULL, imgrefpath__c varchar(255) NULL, crosscontamination__c varchar(255) NULL, imgrefpath_back__c varchar(255) NULL, allergen__c varchar(255) NULL, purpose__c varchar(255) NULL, targetaccounttype__c varchar(255) NULL, sellingpoint__c varchar(255) NULL, CONSTRAINT dkretail__product__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__product__c_systemmodstamp ON salesforce2.dkretail__product__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__product__c_dkretail__productcode__c ON salesforce2.dkretail__product__c USING btree (dkretail__productcode__c);
CREATE UNIQUE INDEX hcu_idx_dkretail__product__c_sfid ON salesforce2.dkretail__product__c USING btree (sfid);

-- salesforce2.dkretail__teammemberschedule__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__teammemberschedule__c;

CREATE TABLE salesforce2.dkretail__teammemberschedule__c ( dkretail__workingtype__c varchar(255) NULL, "name" varchar(80) NULL, dkretail__workingdate__c date NULL, dkretail__altholidayid__c varchar(18) NULL, teamleadersfid__c varchar(100) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__employeeid__c varchar(18) NULL, dkretail__commutelogid__c varchar(18) NULL, dkretail__workingcategory3__c varchar(255) NULL, dkretail__workingcategory2__c varchar(255) NULL, createddate timestamp NULL, dkretail__workingcategory1__c varchar(255) NULL, dkretail__promotionempid__c varchar(18) NULL, commutereportdatetime__c timestamp NULL, accountid__c varchar(18) NULL, workingcategory4__c varchar(255) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, equipment1__c varchar(10) NULL, no_chkcnt__c float8 NULL, id__c varchar(30) NULL, yes_chkcnt__c float8 NULL, precaution_chk__c float8 NULL, starttime__c timestamp NULL, equipment8__c varchar(10) NULL, equipment7__c varchar(10) NULL, equipment6__c varchar(10) NULL, equipment5__c varchar(10) NULL, equipment4__c varchar(10) NULL, completetime__c timestamp NULL, equipment3__c varchar(10) NULL, equipment2__c varchar(10) NULL, precaution__c varchar(3000) NULL, equipment10__c varchar(10) NULL, equipment9__c varchar(10) NULL, traversalflag__c varchar(255) NULL, isworkreport__c varchar(1300) NULL, CONSTRAINT dkretail__teammemberschedule__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__teammemberschedule__c_systemmodstamp ON salesforce2.dkretail__teammemberschedule__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__teammemberschedule__c_sfid ON salesforce2.dkretail__teammemberschedule__c USING btree (sfid);

-- salesforce2.education_code_mng definition

-- Drop table

-- DROP TABLE salesforce2.education_code_mng;

CREATE TABLE salesforce2.education_code_mng ( edu_code varchar(20) NOT NULL, edu_code_nm varchar(50) NULL, edu_type varchar(10) NULL, CONSTRAINT education_code_mng_pkey PRIMARY KEY (edu_code));
COMMENT ON TABLE salesforce2.education_code_mng IS '교육관리 코드 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_code_mng.edu_type IS '분류 유형';

-- salesforce2.education_file_mng definition

-- Drop table

-- DROP TABLE salesforce2.education_file_mng;

CREATE TABLE salesforce2.education_file_mng ( edu_id varchar(20) NULL, edu_file_key varchar(30) NULL, edu_file_type varchar(10) NULL, edu_file_orgnm varchar(200) NULL);
COMMENT ON TABLE salesforce2.education_file_mng IS '교육자료 파일 관리 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_file_mng.edu_id IS '교육자료 ID';
COMMENT ON COLUMN salesforce2.education_file_mng.edu_file_key IS '업로드된 파일 key값';
COMMENT ON COLUMN salesforce2.education_file_mng.edu_file_type IS '업로드된 파일 타입(이미지, 영상, 문서)';

-- salesforce2.education_member_history definition

-- Drop table

-- DROP TABLE salesforce2.education_member_history;

CREATE TABLE salesforce2.education_member_history ( community_id varchar(20) NULL, empcode__c varchar(40) NULL, "name" varchar(80) NULL, costcentercode__c varchar(10) NULL, inst_date timestamp NULL);
COMMENT ON TABLE salesforce2.education_member_history IS '교육자료 및 공지사항 멤버 조회 히스토리 저장 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_member_history.community_id IS '교육자료 및 공지사항 게시글 ID';
COMMENT ON COLUMN salesforce2.education_member_history.empcode__c IS '사번';
COMMENT ON COLUMN salesforce2.education_member_history."name" IS '이름';
COMMENT ON COLUMN salesforce2.education_member_history.costcentercode__c IS '지점코드';
COMMENT ON COLUMN salesforce2.education_member_history.inst_date IS '등록시간';

-- salesforce2.education_mng definition

-- Drop table

-- DROP TABLE salesforce2.education_mng;

CREATE TABLE salesforce2.education_mng ( edu_id varchar(20) NOT NULL, edu_title varchar(150) NULL, edu_content text NULL, edu_code varchar(50) NULL, empcode__c varchar(40) NULL, inst_date timestamp NULL, upd_date timestamp NULL, CONSTRAINT education_mng_pkey PRIMARY KEY (edu_id));
COMMENT ON TABLE salesforce2.education_mng IS '교육자료 관리 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_mng.edu_id IS '교육자료 ID';
COMMENT ON COLUMN salesforce2.education_mng.edu_title IS '교육자료 제목';
COMMENT ON COLUMN salesforce2.education_mng.edu_content IS '교육자료 내용';
COMMENT ON COLUMN salesforce2.education_mng.edu_code IS '교육자료 카테고리';
COMMENT ON COLUMN salesforce2.education_mng.empcode__c IS '등록자';
COMMENT ON COLUMN salesforce2.education_mng.inst_date IS '등록일';
COMMENT ON COLUMN salesforce2.education_mng.upd_date IS '수정일';

-- salesforce2.employee_admin_mng definition

-- Drop table

-- DROP TABLE salesforce2.employee_admin_mng;

CREATE TABLE salesforce2.employee_admin_mng ( empcode__c varchar(40) NOT NULL, CONSTRAINT employee_admin_mng_pkey PRIMARY KEY (empcode__c));
COMMENT ON TABLE salesforce2.employee_admin_mng IS '관리자 접근 계정 관리 테이블';

-- salesforce2.employee_his definition

-- Drop table

-- DROP TABLE salesforce2.employee_his;

CREATE TABLE salesforce2.employee_his ( empcode__c varchar(80) NULL, inst_date timestamp NULL);
COMMENT ON TABLE salesforce2.employee_his IS '로그인 사용장 이력';

-- salesforce2.employee_mng definition

-- Drop table

-- DROP TABLE salesforce2.employee_mng;

CREATE TABLE salesforce2.employee_mng ( empcode__c varchar(40) NOT NULL, emp_pwd varchar(200) NULL, emp_uuid varchar(200) NULL, gps_yn bool NULL, pwd_yn bool NULL, inst_date timestamp NULL, upd_date timestamp NULL, emp_token varchar(200) NULL, gps_yn_date timestamp NULL, CONSTRAINT employee_mng_pkey PRIMARY KEY (empcode__c));
COMMENT ON TABLE salesforce2.employee_mng IS '로그인 사용자 정보';

-- salesforce2.expirationdate__mng definition

-- Drop table

-- DROP TABLE salesforce2.expirationdate__mng;

CREATE TABLE salesforce2.expirationdate__mng ( account_id varchar(100) NULL, account_code varchar(100) NULL, employee_id varchar(100) NULL, product_id varchar(100) NULL, product_code varchar(100) NULL, expiration_date date NULL, alarm_date date NULL, description text NULL, seq int4 DEFAULT nextval('salesforce2.expirationdate__mng_id_seq'::regclass) NOT NULL, inst_dt timestamp NULL, updt_dt timestamp NULL, CONSTRAINT expirationdate__mng_pkey PRIMARY KEY (seq));
COMMENT ON TABLE salesforce2.expirationdate__mng IS '판매사원_유통기한_알림관리 테이블';

-- salesforce2.hqreview__c definition

-- Drop table

-- DROP TABLE salesforce2.hqreview__c;

CREATE TABLE salesforce2.hqreview__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, branchcode__c varchar(100) NULL, branchname__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, firstdayofmonth__c date NULL, evaluationytype__c varchar(255) NULL, abctypecode__c varchar(255) NULL, hr_code_c__c varchar(255) NULL, CONSTRAINT hqreview__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_hqreview__c_systemmodstamp ON salesforce2.hqreview__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_hqreview__c_sfid ON salesforce2.hqreview__c USING btree (sfid);

-- salesforce2.if_product__c definition

-- Drop table

-- DROP TABLE salesforce2.if_product__c;

CREATE TABLE salesforce2.if_product__c ( dkretail__orderingunit__c varchar(40) NULL, dkretail__shelflifeunit__c varchar(40) NULL, dkretail__producttype__c varchar(255) NULL, dkretail__standardunitprice__c float8 NULL, dkretail__categorycode3__c varchar(100) NULL, dkretail__categorycode2__c varchar(100) NULL, dkretail__shelflife__c varchar(30) NULL, dkretail__categorycode1__c varchar(100) NULL, "name" varchar(80) NULL, dkretail__logisticsbarcode__c varchar(100) NULL, dkretail__productstatus__c varchar(255) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__productcode__c varchar(100) NULL, dkretail__unit__c varchar(40) NULL, createddate timestamp NULL, dkretail__category3__c varchar(255) NULL, dkretail__launchdate__c date NULL, dkretail__conversionquantity__c float8 NULL, dkretail__boxreceivingquantity__c float8 NULL, dkretail__category2__c varchar(255) NULL, dkretail__category1__c varchar(255) NULL, dkretail__storecondition__c varchar(255) NULL, sfid varchar(18) NULL, id int4 NOT NULL, supertax__c float8 NULL, tastegift__c varchar(1) NULL, allergen__c text NULL, sellingpoint__c text NULL, purpose__c text NULL, imgrefpathtxt__c text NULL, updateflag__c bool NULL, imgrefpath__c varchar(255) NULL, crosscontamination__c text NULL, productfeatures__c text NULL, imgrefpath_front__c text NULL, imgrefpath_back__c text NULL, targetaccounttype__c text NULL, CONSTRAINT if_product__c_pkey PRIMARY KEY (id));

-- salesforce2.monthlysaleshistory__c definition

-- Drop table

-- DROP TABLE salesforce2.monthlysaleshistory__c;

CREATE TABLE salesforce2.monthlysaleshistory__c ( account_externalkey__c varchar(1300) NULL, lastmonthtargetfomula__c float8 NULL, account_branchname__c varchar(1300) NULL, lastmonthtargetachievedratio__c float8 NULL, fridgepurpose__c float8 NULL, "name" varchar(80) NULL, fm_year__c float8 NULL, salesmonth__c varchar(255) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, shipclosingamount__c float8 NULL, createddate timestamp NULL, lastmonthresults__c float8 NULL, account_type__c varchar(1300) NULL, salesyear__c varchar(255) NULL, abcclosingamount3__c float8 NULL, fm_month__c float8 NULL, targetmonthresults__c float8 NULL, abcclosingamount2__c float8 NULL, abcclosingamount1__c float8 NULL, ambientpurpose__c float8 NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT monthlysaleshistory__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_monthlysaleshistory__c_systemmodstamp ON salesforce2.monthlysaleshistory__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_monthlysaleshistory__c_sfid ON salesforce2.monthlysaleshistory__c USING btree (sfid);

-- salesforce2.product_favorites definition

-- Drop table

-- DROP TABLE salesforce2.product_favorites;

CREATE TABLE salesforce2.product_favorites ( employeecode varchar(80) NULL, productcode varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL);
COMMENT ON TABLE salesforce2.product_favorites IS '즐겨찾기';

-- salesforce2.productbarcode__c definition

-- Drop table

-- DROP TABLE salesforce2.productbarcode__c;

CREATE TABLE salesforce2.productbarcode__c ( productname__c varchar(255) NULL, "name" varchar(80) NULL, isdeleted bool NULL, productunit__c varchar(255) NULL, systemmodstamp timestamp NULL, productbarcode__c varchar(255) NULL, productsequence__c varchar(255) NULL, product__c varchar(18) NULL, createddate timestamp NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT productbarcode__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_productbarcode__c_systemmodstamp ON salesforce2.productbarcode__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_productbarcode__c_sfid ON salesforce2.productbarcode__c USING btree (sfid);

-- salesforce2.pushmessage__c definition

-- Drop table

-- DROP TABLE salesforce2.pushmessage__c;

CREATE TABLE salesforce2.pushmessage__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, message__c varchar(500) NULL, scheduledate__c timestamp NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT pushmessage__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_pushmessage__c_systemmodstamp ON salesforce2.pushmessage__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_pushmessage__c_sfid ON salesforce2.pushmessage__c USING btree (sfid);

-- salesforce2.pushmessagereceiver__c definition

-- Drop table

-- DROP TABLE salesforce2.pushmessagereceiver__c;

CREATE TABLE salesforce2.pushmessagereceiver__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, employeeid__c varchar(18) NULL, messageid__c varchar(18) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT pushmessagereceiver__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_pushmessagereceiver__c_systemmodstamp ON salesforce2.pushmessagereceiver__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_pushmessagereceiver__c_sfid ON salesforce2.pushmessagereceiver__c USING btree (sfid);

-- salesforce2.safetycheck__workschedule__member definition

-- Drop table

-- DROP TABLE salesforce2.safetycheck__workschedule__member;

CREATE TABLE salesforce2.safetycheck__workschedule__member ( "masterId" varchar(18) NULL, employeeid__c varchar(18) NULL, working__date date NULL, starttime timestamp NULL, completetime timestamp NULL, yes_chkcnt float8 NULL, no_chkcnt float8 NULL, equipment1 varchar(10) NULL, equipment2 varchar(10) NULL, equipment3 varchar(10) NULL, equipment4 varchar(10) NULL, equipment5 varchar(10) NULL, equipment6 varchar(10) NULL, equipment7 varchar(10) NULL, equipment8 varchar(10) NULL, equipment9 varchar(10) NULL, precaution varchar(3000) NULL, precaution_chkcnt float8 NULL, traversalflag varchar(255) NULL, eventmasterid varchar(18) NULL, completeworkyn varchar(18) DEFAULT 'N'::character varying NULL);
COMMENT ON TABLE salesforce2.safetycheck__workschedule__member IS '안전점검 체크 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member."masterId" IS '진열마스터  name';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.employeeid__c IS '사원 sfid';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.working__date IS '안전점검 일자';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.starttime IS '안전점검 시작일자';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.completetime IS '안전점검 완료일자';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.yes_chkcnt IS '1항목 예 체크 개수';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.no_chkcnt IS '1항목 해당없음 체크 개수';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment1 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment2 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment3 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment4 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment5 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment6 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment7 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment8 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.equipment9 IS '1항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.precaution IS '2항목 질문';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.precaution_chkcnt IS '2항목 질문 체크 개수';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.traversalflag IS '순회여부 flag';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.eventmasterid IS '행사마스터 sfid';
COMMENT ON COLUMN salesforce2.safetycheck__workschedule__member.completeworkyn IS '출근등록 완료 여부';

-- salesforce2.safetycheck_list definition

-- Drop table

-- DROP TABLE salesforce2.safetycheck_list;

CREATE TABLE salesforce2.safetycheck_list ( question_num int4 NOT NULL, seq_num int4 NOT NULL, contents varchar(500) NOT NULL, use_yn varchar(1) DEFAULT 'Y'::character varying NULL, CONSTRAINT safetycheck_list_pk PRIMARY KEY (question_num, seq_num));
COMMENT ON TABLE salesforce2.safetycheck_list IS '안전점검리스트';

-- Column comments

COMMENT ON COLUMN salesforce2.safetycheck_list.question_num IS '질문 문항 ex)질문1, 질문2';
COMMENT ON COLUMN salesforce2.safetycheck_list.seq_num IS '문항순서, 체크리스트의 정렬순서';
COMMENT ON COLUMN salesforce2.safetycheck_list.contents IS '점검내용';
COMMENT ON COLUMN salesforce2.safetycheck_list.use_yn IS '사용여부';

-- salesforce2.staffreview__c definition

-- Drop table

-- DROP TABLE salesforce2.staffreview__c;

CREATE TABLE salesforce2.staffreview__c ( branch__c varchar(1300) NULL, dkretail_employeeid__c varchar(18) NULL, instructionsdefault__c float8 NULL, employeetotalscore__c float8 NULL, branchreviews__c varchar(18) NULL, employeename__c varchar(1300) NULL, "name" varchar(80) NULL, priority_eventitemmanage__c float8 NULL, employeenumber__c varchar(1300) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, clothessatellite__c float8 NULL, productmanagecallment__c float8 NULL, educationalevaluation__c float8 NULL, costcentercode__c varchar(1300) NULL, displaymanageeventgoals__c float8 NULL, createddate timestamp NULL, businesspartnerties__c float8 NULL, attendance__c float8 NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT staffreview__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_staffreview__c_systemmodstamp ON salesforce2.staffreview__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_staffreview__c_sfid ON salesforce2.staffreview__c USING btree (sfid);

-- salesforce2.theme__c definition

-- Drop table

-- DROP TABLE salesforce2.theme__c;

CREATE TABLE salesforce2.theme__c ( "name" varchar(80) NULL, title__c varchar(250) NULL, publicflag__c bool NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, department__c varchar(100) NULL, startdate__c date NULL, createddate timestamp NULL, enddate__c date NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, branchcode__c varchar(30) NULL, CONSTRAINT theme__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_theme__c_systemmodstamp ON salesforce2.theme__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_theme__c_sfid ON salesforce2.theme__c USING btree (sfid);

-- salesforce2.tmp_claim definition

-- Drop table

-- DROP TABLE salesforce2.tmp_claim;

CREATE TABLE salesforce2.tmp_claim ( tmp_sapaccountname varchar(80) NULL, tmp_sapaccountcode varchar(80) NULL, tmp_productname varchar(100) NULL, tmp_productcode varchar(80) NULL, tmp_expirationdate varchar(80) NULL, tmp_claimtype1 varchar(80) NULL, tmp_claimtype2 varchar(80) NULL, tmp_description text NULL, tmp_quantity varchar(80) NULL, tmp_claimimagefilename varchar(200) NULL, tmp_partimagefilename varchar(200) NULL, tmp_amount varchar(80) NULL, tmp_purchasemethod varchar(80) NULL, tmp_receiptimagefilename varchar(200) NULL, tmp_requesttype text NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_employeecode varchar(80) NULL, tmp_claimtype1_name varchar(80) NULL, tmp_claimtype2_name varchar(80) NULL, tmp_purchasecode varchar(80) NULL, tmp_claimimageextension varchar(80) NULL, tmp_partimageextension varchar(80) NULL, tmp_receiptimageextension varchar(80) NULL, tmp_receiptimagebuffer text NULL, tmp_partimagebuffer text NULL, tmp_claimimagebuffer text NULL, tmp_manufacturingdate varchar(80) NULL);
COMMENT ON TABLE salesforce2.tmp_claim IS '클레임 임시저장 테이블';

-- salesforce2.tmp_claimcode definition

-- Drop table

-- DROP TABLE salesforce2.tmp_claimcode;

CREATE TABLE salesforce2.tmp_claimcode ( claim1_code varchar(80) NULL, claim1_name varchar(80) NULL, claim2_code varchar(80) NULL, claim2_name varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL);
COMMENT ON TABLE salesforce2.tmp_claimcode IS '클레임 카테고리 코드';

-- salesforce2.tmp_onsite definition

-- Drop table

-- DROP TABLE salesforce2.tmp_onsite;

CREATE TABLE salesforce2.tmp_onsite ( tmp_employeecode varchar(80) NULL, tmp_themecode varchar(80) NULL, tmp_themename varchar(80) NULL, tmp_classification varchar(80) NULL, tmp_sapaccountname varchar(100) NULL, tmp_sapaccoutncode varchar(80) NULL, tmp_category varchar(100) NULL, tmp_activitydate varchar(80) NULL, tmp_description text NULL, tmp_productname varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_competitorname varchar(80) NULL, tmp_competitoractivity text NULL, tmp_sampletastflag bpchar(1) NULL, tmp_competitorproudctname varchar(80) NULL, tmp_sampletasterprice varchar(80) NULL, tmp_activityamount varchar(40) NULL, tmp_s3imagekey1 varchar(255) NULL, tmp_s3imagekey2 varchar(255) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_s3imagefilename1 varchar(80) NULL, tmp_s3imagefilesize1 varchar(80) NULL, tmp_s3imagefilename2 varchar(80) NULL, tmp_s3imagefilesize2 varchar(80) NULL);

-- salesforce2.tmp_order definition

-- Drop table

-- DROP TABLE salesforce2.tmp_order;

CREATE TABLE salesforce2.tmp_order ( tmp_employeecode varchar(80) NOT NULL, tmp_accountcode varchar(80) NULL, tmp_orderdate date NULL, tmp_totalamount varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, CONSTRAINT tmp_order_pkey PRIMARY KEY (tmp_employeecode));
COMMENT ON TABLE salesforce2.tmp_order IS '주문서 작성 임시저장 테이블';

-- salesforce2.tmp_order_product definition

-- Drop table

-- DROP TABLE salesforce2.tmp_order_product;

CREATE TABLE salesforce2.tmp_order_product ( tmp_employeecode varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_boxcnt varchar(80) NULL, tmp_eacnt varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_totalcnt varchar(80) NULL);
COMMENT ON TABLE salesforce2.tmp_order_product IS '주문서작성 제품 임시저장 테이블';

-- salesforce2.tmp_promotion definition

-- Drop table

-- DROP TABLE salesforce2.tmp_promotion;

CREATE TABLE salesforce2.tmp_promotion ( tmp_employeecode varchar(80) NULL, tmp_promotiontype varchar(80) NULL, tmp_promotionname varchar(100) NULL, tmp_promotionproductname varchar(80) NULL, tmp_promotionproductcode varchar(80) NULL, tmp_baseprice varchar(80) NULL, tmp_primaryquantity varchar(80) NULL, tmp_otherproduct varchar(80) NULL, tmp_otherquantity varchar(80) NULL, tmp_othertotalamount varchar(80) NULL, tmp_imageurl varchar(200) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_promotion_id varchar(80) NULL, tmp_promotion_seq varchar(80) NULL, tmp_imagefilename varchar(80) NULL, tmp_otherchangeproduct varchar(80) NULL);

-- salesforce2.tmp_suggest definition

-- Drop table

-- DROP TABLE salesforce2.tmp_suggest;

CREATE TABLE salesforce2.tmp_suggest ( tmp_category varchar(80) NULL, tmp_productname varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_title varchar(100) NULL, tmp_description text NULL, tmp_employeecode varchar(80) NULL, tmp_s3imageurl1 varchar(80) NULL, tmp_s3imageurl2 varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_s3imagefilename1 varchar(80) NULL, tmp_s3imagefilename2 varchar(80) NULL, tmp_s3imagefilesize1 varchar(80) NULL, tmp_s3imagefilesize2 varchar(80) NULL, tmp_carnumber text NULL, tmp_accountcode text NULL, tmp_claimlist text NULL, tmp_claimdate date NULL);
COMMENT ON TABLE salesforce2.tmp_suggest IS '제안하기 임시저장 테이블';

-- salesforce2.uploadfile__c definition

-- Drop table

-- DROP TABLE salesforce2.uploadfile__c;

CREATE TABLE salesforce2.uploadfile__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, recordid__c varchar(40) NULL, uniquekey__c varchar(100) NULL, size__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT uploadfile__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_uploadfile__c_systemmodstamp ON salesforce2.uploadfile__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_uploadfile__c_sfid ON salesforce2.uploadfile__c USING btree (sfid);
