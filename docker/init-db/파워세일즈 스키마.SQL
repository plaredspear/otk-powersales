-- DROP SCHEMA salesforce2;

CREATE SCHEMA salesforce2 AUTHORIZATION u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2._hcmeta_id_seq;

CREATE SEQUENCE salesforce2._hcmeta_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2._hcmeta_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2._hcmeta_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2._sf_event_log_id_seq;

CREATE SEQUENCE salesforce2._sf_event_log_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2._sf_event_log_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2._sf_event_log_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2._trigger_log_id_seq;

CREATE SEQUENCE salesforce2._trigger_log_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2._trigger_log_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2._trigger_log_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.account_id_seq;

CREATE SEQUENCE salesforce2.account_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.account_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.account_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.agreementword__c_id_seq;

CREATE SEQUENCE salesforce2.agreementword__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.agreementword__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.agreementword__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq;

CREATE SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.displayworkschedulemaster__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.dkretail__employee__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__employee__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.dkretail__employee__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.dkretail__employee__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.dkretail__notice__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__notice__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.dkretail__notice__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.dkretail__notice__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.dkretail__product__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__product__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.dkretail__product__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.dkretail__product__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq;

CREATE SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.dkretail__teammemberschedule__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.expirationdate__mng_id_seq;

CREATE SEQUENCE salesforce2.expirationdate__mng_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.expirationdate__mng_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.expirationdate__mng_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.hqreview__c_id_seq;

CREATE SEQUENCE salesforce2.hqreview__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.hqreview__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.hqreview__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.monthlysaleshistory__c_id_seq;

CREATE SEQUENCE salesforce2.monthlysaleshistory__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.monthlysaleshistory__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.monthlysaleshistory__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.productbarcode__c_id_seq;

CREATE SEQUENCE salesforce2.productbarcode__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.productbarcode__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.productbarcode__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.pushmessage__c_id_seq;

CREATE SEQUENCE salesforce2.pushmessage__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.pushmessage__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.pushmessage__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.pushmessagereceiver__c_id_seq;

CREATE SEQUENCE salesforce2.pushmessagereceiver__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.pushmessagereceiver__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.pushmessagereceiver__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.staffreview__c_id_seq;

CREATE SEQUENCE salesforce2.staffreview__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.staffreview__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.staffreview__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.theme__c_id_seq;

CREATE SEQUENCE salesforce2.theme__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.theme__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.theme__c_id_seq TO u4bee3ek26k44g;

-- DROP SEQUENCE salesforce2.uploadfile__c_id_seq;

CREATE SEQUENCE salesforce2.uploadfile__c_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 2147483647
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE salesforce2.uploadfile__c_id_seq OWNER TO u4bee3ek26k44g;
GRANT ALL ON SEQUENCE salesforce2.uploadfile__c_id_seq TO u4bee3ek26k44g;
-- salesforce2._hcmeta definition

-- Drop table

-- DROP TABLE salesforce2._hcmeta;

CREATE TABLE salesforce2._hcmeta ( id serial4 NOT NULL, hcver int4 NULL, org_id varchar(50) NULL, details text NULL, CONSTRAINT _hcmeta_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE salesforce2._hcmeta OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2._hcmeta TO u4bee3ek26k44g;


-- salesforce2._sf_event_log definition

-- Drop table

-- DROP TABLE salesforce2._sf_event_log;

CREATE TABLE salesforce2._sf_event_log ( id serial4 NOT NULL, table_name varchar(128) NULL, "action" varchar(7) NULL, synced_at timestamptz DEFAULT now() NULL, sf_timestamp timestamptz NULL, sfid varchar(20) NULL, record text NULL, processed bool NULL, CONSTRAINT _sf_event_log_pkey PRIMARY KEY (id));
CREATE INDEX idx__sf_event_log_comp_key ON salesforce2._sf_event_log USING btree (table_name, synced_at);
CREATE INDEX idx__sf_event_log_sfid ON salesforce2._sf_event_log USING btree (sfid);

-- Permissions

ALTER TABLE salesforce2._sf_event_log OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2._sf_event_log TO u4bee3ek26k44g;


-- salesforce2._trigger_log definition

-- Drop table

-- DROP TABLE salesforce2._trigger_log;

CREATE TABLE salesforce2._trigger_log ( id serial4 NOT NULL, txid int8 NULL, created_at timestamptz DEFAULT now() NULL, updated_at timestamptz DEFAULT now() NULL, processed_at timestamptz NULL, processed_tx int8 NULL, state varchar(8) NULL, "action" varchar(7) NULL, table_name varchar(128) NULL, record_id int4 NULL, sfid varchar(18) NULL, "old" text NULL, "values" text NULL, sf_result int4 NULL, sf_message text NULL, CONSTRAINT _trigger_log_pkey PRIMARY KEY (id));
CREATE INDEX _trigger_log_idx_created_at ON salesforce2._trigger_log USING btree (created_at);
CREATE INDEX _trigger_log_idx_state_id ON salesforce2._trigger_log USING btree (state, id);
CREATE INDEX _trigger_log_idx_state_table_name ON salesforce2._trigger_log USING btree (state, table_name) WHERE (((state)::text = 'NEW'::text) OR ((state)::text = 'PENDING'::text));

-- Table Triggers

create trigger tlog_insert_trigger after
insert
    on
    salesforce2._trigger_log for each row execute function salesforce2.tlog_notify_trigger();

-- Permissions

ALTER TABLE salesforce2._trigger_log OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2._trigger_log TO u4bee3ek26k44g;


-- salesforce2._trigger_log_archive definition

-- Drop table

-- DROP TABLE salesforce2._trigger_log_archive;

CREATE TABLE salesforce2._trigger_log_archive ( id int4 NOT NULL, txid int8 NULL, created_at timestamptz NULL, updated_at timestamptz NULL, processed_at timestamptz NULL, processed_tx int8 NULL, state varchar(8) NULL, "action" varchar(7) NULL, table_name varchar(128) NULL, record_id int4 NULL, sfid varchar(18) NULL, "old" text NULL, "values" text NULL, sf_result int4 NULL, sf_message text NULL, CONSTRAINT _trigger_log_archive_pkey PRIMARY KEY (id));
CREATE INDEX _trigger_log_archive_idx_created_at ON salesforce2._trigger_log_archive USING btree (created_at);
CREATE INDEX _trigger_log_archive_idx_record_id ON salesforce2._trigger_log_archive USING btree (record_id);
CREATE INDEX _trigger_log_archive_idx_state_table_name ON salesforce2._trigger_log_archive USING btree (state, table_name) WHERE ((state)::text = 'FAILED'::text);

-- Permissions

ALTER TABLE salesforce2._trigger_log_archive OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2._trigger_log_archive TO u4bee3ek26k44g;


-- salesforce2.account definition

-- Drop table

-- DROP TABLE salesforce2.account;

CREATE TABLE salesforce2.account ( address2__c varchar(120) NULL, address1__c varchar(120) NULL, representative__c varchar(100) NULL, abctype__c varchar(20) NULL, "name" varchar(255) NULL, phone varchar(40) NULL, externalkey__c varchar(100) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, accountgroup__c varchar(10) NULL, closingtime3__c varchar(50) NULL, closingtime2__c varchar(50) NULL, longitude__c varchar(100) NULL, closingtime1__c varchar(50) NULL, branchcode__c varchar(100) NULL, zipcode__c varchar(100) NULL, branchname__c varchar(250) NULL, createddate timestamp NULL, latitude__c varchar(100) NULL, mobilephone__c varchar(40) NULL, abctypecode__c varchar(40) NULL, industry varchar(255) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, werk1_tx__c varchar(255) NULL, werk2_tx__c varchar(255) NULL, werk3_tx__c varchar(255) NULL, CONSTRAINT account_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_account_systemmodstamp ON salesforce2.account USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_account_sfid ON salesforce2.account USING btree (sfid);

-- Table Triggers

create trigger hc_account_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.account for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_account_logger();
create trigger hc_account_status_trigger before
insert
    or
update
    on
    salesforce2.account for each row execute function salesforce2.hc_account_status();

-- Permissions

ALTER TABLE salesforce2.account OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.account TO u4bee3ek26k44g;


-- salesforce2.agreementword__c definition

-- Drop table

-- DROP TABLE salesforce2.agreementword__c;

CREATE TABLE salesforce2.agreementword__c ( contents__c varchar(8000) NULL, "name" varchar(80) NULL, activedate__c date NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, afteractivedate__c date NULL, createddate timestamp NULL, active__c bool NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT agreementword__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_agreementword__c_systemmodstamp ON salesforce2.agreementword__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_agreementword__c_sfid ON salesforce2.agreementword__c USING btree (sfid);

-- Table Triggers

create trigger hc_agreementword__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.agreementword__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_agreementword__c_logger();
create trigger hc_agreementword__c_status_trigger before
insert
    or
update
    on
    salesforce2.agreementword__c for each row execute function salesforce2.hc_agreementword__c_status();

-- Permissions

ALTER TABLE salesforce2.agreementword__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.agreementword__c TO u4bee3ek26k44g;


-- salesforce2.commute_distance definition

-- Drop table

-- DROP TABLE salesforce2.commute_distance;

CREATE TABLE salesforce2.commute_distance ( distance int4 NULL);
COMMENT ON TABLE salesforce2.commute_distance IS '출근거리';

-- Permissions

ALTER TABLE salesforce2.commute_distance OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.commute_distance TO u4bee3ek26k44g;


-- salesforce2.device_version_mng definition

-- Drop table

-- DROP TABLE salesforce2.device_version_mng;

CREATE TABLE salesforce2.device_version_mng ( "version" varchar(10) NOT NULL, device varchar(10) NOT NULL, createdate timestamp NOT NULL, contents varchar(1000) NOT NULL, s3_key varchar(200) NOT NULL, file_url varchar(300) NULL, s3_key_ipa varchar(200) NULL, file_url_ipa varchar(300) NULL, CONSTRAINT device_version_mng_pk PRIMARY KEY (version, device));

-- Permissions

ALTER TABLE salesforce2.device_version_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.device_version_mng TO u4bee3ek26k44g;


-- salesforce2.displayworkschedulemaster__c definition

-- Drop table

-- DROP TABLE salesforce2.displayworkschedulemaster__c;

CREATE TABLE salesforce2.displayworkschedulemaster__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, account__c varchar(18) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, confirmed__c bool NULL, fullname__c varchar(18) NULL, typeofwork5__c varchar(255) NULL, typeofwork3__c varchar(255) NULL, startdate__c date NULL, typeofwork1__c varchar(255) NULL, enddate__c date NULL, createdbyid varchar(18) NULL, ownerid varchar(18) NULL, CONSTRAINT displayworkschedulemaster__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_displayworkschedulemaster__c_systemmodstamp ON salesforce2.displayworkschedulemaster__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_displayworkschedulemaster__c_sfid ON salesforce2.displayworkschedulemaster__c USING btree (sfid);

-- Table Triggers

create trigger hc_displayworkschedulemaster__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.displayworkschedulemaster__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_displayworkschedulemaster__c_logger();
create trigger hc_displayworkschedulemaster__c_status_trigger before
insert
    or
update
    on
    salesforce2.displayworkschedulemaster__c for each row execute function salesforce2.hc_displayworkschedulemaster__c_status();

-- Permissions

ALTER TABLE salesforce2.displayworkschedulemaster__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.displayworkschedulemaster__c TO u4bee3ek26k44g;


-- salesforce2.dkretail__employee__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__employee__c;

CREATE TABLE salesforce2.dkretail__employee__c ( dkretail__birthdate__c varchar(10) NULL, dkretail__status__c varchar(40) NULL, dkretail__apploginactive__c bool NULL, "name" varchar(80) NULL, agreementflag__c bool NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__workphone__c varchar(255) NULL, costcentercode__c varchar(10) NULL, phone__c varchar(40) NULL, createddate timestamp NULL, dkretail__appauthority__c varchar(255) NULL, dkretail__empcode__c varchar(100) NULL, dkretail__startdate__c date NULL, dkretail__homephone__c varchar(255) NULL, dkretail__orgname__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT dkretail__employee__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__employee__c_systemmodstamp ON salesforce2.dkretail__employee__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__employee__c_sfid ON salesforce2.dkretail__employee__c USING btree (sfid);

-- Table Triggers

create trigger hc_dkretail__employee__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.dkretail__employee__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_dkretail__employee__c_logger();
create trigger hc_dkretail__employee__c_status_trigger before
insert
    or
update
    on
    salesforce2.dkretail__employee__c for each row execute function salesforce2.hc_dkretail__employee__c_status();

-- Permissions

ALTER TABLE salesforce2.dkretail__employee__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.dkretail__employee__c TO u4bee3ek26k44g;


-- salesforce2.dkretail__notice__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__notice__c;

CREATE TABLE salesforce2.dkretail__notice__c ( dkretail__jeejum__c varchar(255) NULL, "name" varchar(80) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__jeejumcode__c varchar(255) NULL, createddate timestamp NULL, dkretail__scope__c varchar(255) NULL, dkretail__category__c varchar(255) NULL, dkretail__contents__c text NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, dkretail__educategory__c varchar(255) NULL, employeeid__c varchar(18) NULL, CONSTRAINT dkretail__notice__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__notice__c_employeeid__c ON salesforce2.dkretail__notice__c USING btree (employeeid__c);
CREATE INDEX hc_idx_dkretail__notice__c_systemmodstamp ON salesforce2.dkretail__notice__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__notice__c_sfid ON salesforce2.dkretail__notice__c USING btree (sfid);

-- Table Triggers

create trigger hc_dkretail__notice__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.dkretail__notice__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_dkretail__notice__c_logger();
create trigger hc_dkretail__notice__c_status_trigger before
insert
    or
update
    on
    salesforce2.dkretail__notice__c for each row execute function salesforce2.hc_dkretail__notice__c_status();

-- Permissions

ALTER TABLE salesforce2.dkretail__notice__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.dkretail__notice__c TO u4bee3ek26k44g;


-- salesforce2.dkretail__product__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__product__c;

CREATE TABLE salesforce2.dkretail__product__c ( dkretail__orderingunit__c varchar(40) NULL, dkretail__shelflifeunit__c varchar(40) NULL, dkretail__producttype__c varchar(255) NULL, dkretail__standardunitprice__c float8 NULL, dkretail__categorycode3__c varchar(100) NULL, dkretail__categorycode2__c varchar(100) NULL, dkretail__categorycode1__c varchar(100) NULL, "name" varchar(80) NULL, dkretail__productstatus__c varchar(255) NULL, shelflifefull__c varchar(1300) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__productcode__c varchar(100) NULL, supertax__c float8 NULL, dkretail__unit__c varchar(40) NULL, createddate timestamp NULL, standardprice__c float8 NULL, dkretail__category3__c varchar(255) NULL, dkretail__launchdate__c date NULL, dkretail__conversionquantity__c float8 NULL, dkretail__boxreceivingquantity__c float8 NULL, dkretail__category2__c varchar(255) NULL, dkretail__category1__c varchar(255) NULL, tastegift__c varchar(1) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, dkretail__shelflife__c varchar(30) NULL, dkretail__storecondition__c varchar(255) NULL, productfeatures__c varchar(255) NULL, imgrefpath_front__c varchar(255) NULL, imgrefpathtxt__c varchar(255) NULL, dkretail__logisticsbarcode__c varchar(100) NULL, imgrefpath__c varchar(255) NULL, crosscontamination__c varchar(255) NULL, imgrefpath_back__c varchar(255) NULL, allergen__c varchar(255) NULL, purpose__c varchar(255) NULL, targetaccounttype__c varchar(255) NULL, sellingpoint__c varchar(255) NULL, CONSTRAINT dkretail__product__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__product__c_systemmodstamp ON salesforce2.dkretail__product__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__product__c_dkretail__productcode__c ON salesforce2.dkretail__product__c USING btree (dkretail__productcode__c);
CREATE UNIQUE INDEX hcu_idx_dkretail__product__c_sfid ON salesforce2.dkretail__product__c USING btree (sfid);

-- Table Triggers

create trigger hc_dkretail__product__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.dkretail__product__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_dkretail__product__c_logger();
create trigger hc_dkretail__product__c_status_trigger before
insert
    or
update
    on
    salesforce2.dkretail__product__c for each row execute function salesforce2.hc_dkretail__product__c_status();
create trigger if__product after
insert
    on
    salesforce2.dkretail__product__c for each row execute function salesforce2.trigger_product_data();

COMMENT ON TRIGGER if__product ON salesforce2.dkretail__product__c IS 'IF_product 테이블에 삽입하기위함';

-- Permissions

ALTER TABLE salesforce2.dkretail__product__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.dkretail__product__c TO u4bee3ek26k44g;


-- salesforce2.dkretail__teammemberschedule__c definition

-- Drop table

-- DROP TABLE salesforce2.dkretail__teammemberschedule__c;

CREATE TABLE salesforce2.dkretail__teammemberschedule__c ( dkretail__workingtype__c varchar(255) NULL, "name" varchar(80) NULL, dkretail__workingdate__c date NULL, dkretail__altholidayid__c varchar(18) NULL, teamleadersfid__c varchar(100) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__employeeid__c varchar(18) NULL, dkretail__commutelogid__c varchar(18) NULL, dkretail__workingcategory3__c varchar(255) NULL, dkretail__workingcategory2__c varchar(255) NULL, createddate timestamp NULL, dkretail__workingcategory1__c varchar(255) NULL, dkretail__promotionempid__c varchar(18) NULL, commutereportdatetime__c timestamp NULL, accountid__c varchar(18) NULL, workingcategory4__c varchar(255) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, equipment1__c varchar(10) NULL, no_chkcnt__c float8 NULL, id__c varchar(30) NULL, yes_chkcnt__c float8 NULL, precaution_chk__c float8 NULL, starttime__c timestamp NULL, equipment8__c varchar(10) NULL, equipment7__c varchar(10) NULL, equipment6__c varchar(10) NULL, equipment5__c varchar(10) NULL, equipment4__c varchar(10) NULL, completetime__c timestamp NULL, equipment3__c varchar(10) NULL, equipment2__c varchar(10) NULL, precaution__c varchar(3000) NULL, equipment10__c varchar(10) NULL, equipment9__c varchar(10) NULL, traversalflag__c varchar(255) NULL, isworkreport__c varchar(1300) NULL, CONSTRAINT dkretail__teammemberschedule__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_dkretail__teammemberschedule__c_systemmodstamp ON salesforce2.dkretail__teammemberschedule__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_dkretail__teammemberschedule__c_sfid ON salesforce2.dkretail__teammemberschedule__c USING btree (sfid);

-- Table Triggers

create trigger hc_dkretail__teammemberschedule__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.dkretail__teammemberschedule__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_dkretail__teammemberschedule__c_logger();
create trigger hc_dkretail__teammemberschedule__c_status_trigger before
insert
    or
update
    on
    salesforce2.dkretail__teammemberschedule__c for each row execute function salesforce2.hc_dkretail__teammemberschedule__c_status();

-- Permissions

ALTER TABLE salesforce2.dkretail__teammemberschedule__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.dkretail__teammemberschedule__c TO u4bee3ek26k44g;


-- salesforce2.education_code_mng definition

-- Drop table

-- DROP TABLE salesforce2.education_code_mng;

CREATE TABLE salesforce2.education_code_mng ( edu_code varchar(20) NOT NULL, edu_code_nm varchar(50) NULL, edu_type varchar(10) NULL, CONSTRAINT education_code_mng_pkey PRIMARY KEY (edu_code));
COMMENT ON TABLE salesforce2.education_code_mng IS '교육관리 코드 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_code_mng.edu_type IS '분류 유형';

-- Permissions

ALTER TABLE salesforce2.education_code_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.education_code_mng TO u4bee3ek26k44g;


-- salesforce2.education_file_mng definition

-- Drop table

-- DROP TABLE salesforce2.education_file_mng;

CREATE TABLE salesforce2.education_file_mng ( edu_id varchar(20) NULL, edu_file_key varchar(30) NULL, edu_file_type varchar(10) NULL, edu_file_orgnm varchar(200) NULL);
COMMENT ON TABLE salesforce2.education_file_mng IS '교육자료 파일 관리 테이블';

-- Column comments

COMMENT ON COLUMN salesforce2.education_file_mng.edu_id IS '교육자료 ID';
COMMENT ON COLUMN salesforce2.education_file_mng.edu_file_key IS '업로드된 파일 key값';
COMMENT ON COLUMN salesforce2.education_file_mng.edu_file_type IS '업로드된 파일 타입(이미지, 영상, 문서)';

-- Permissions

ALTER TABLE salesforce2.education_file_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.education_file_mng TO u4bee3ek26k44g;


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

-- Permissions

ALTER TABLE salesforce2.education_member_history OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.education_member_history TO u4bee3ek26k44g;


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

-- Permissions

ALTER TABLE salesforce2.education_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.education_mng TO u4bee3ek26k44g;


-- salesforce2.employee_admin_mng definition

-- Drop table

-- DROP TABLE salesforce2.employee_admin_mng;

CREATE TABLE salesforce2.employee_admin_mng ( empcode__c varchar(40) NOT NULL, CONSTRAINT employee_admin_mng_pkey PRIMARY KEY (empcode__c));
COMMENT ON TABLE salesforce2.employee_admin_mng IS '관리자 접근 계정 관리 테이블';

-- Permissions

ALTER TABLE salesforce2.employee_admin_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.employee_admin_mng TO u4bee3ek26k44g;


-- salesforce2.employee_his definition

-- Drop table

-- DROP TABLE salesforce2.employee_his;

CREATE TABLE salesforce2.employee_his ( empcode__c varchar(80) NULL, inst_date timestamp NULL);
COMMENT ON TABLE salesforce2.employee_his IS '로그인 사용장 이력';

-- Permissions

ALTER TABLE salesforce2.employee_his OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.employee_his TO u4bee3ek26k44g;


-- salesforce2.employee_mng definition

-- Drop table

-- DROP TABLE salesforce2.employee_mng;

CREATE TABLE salesforce2.employee_mng ( empcode__c varchar(40) NOT NULL, emp_pwd varchar(200) NULL, emp_uuid varchar(200) NULL, gps_yn bool NULL, pwd_yn bool NULL, inst_date timestamp NULL, upd_date timestamp NULL, emp_token varchar(200) NULL, gps_yn_date timestamp NULL, CONSTRAINT employee_mng_pkey PRIMARY KEY (empcode__c));
COMMENT ON TABLE salesforce2.employee_mng IS '로그인 사용자 정보';

-- Permissions

ALTER TABLE salesforce2.employee_mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.employee_mng TO u4bee3ek26k44g;


-- salesforce2.expirationdate__mng definition

-- Drop table

-- DROP TABLE salesforce2.expirationdate__mng;

CREATE TABLE salesforce2.expirationdate__mng ( account_id varchar(100) NULL, account_code varchar(100) NULL, employee_id varchar(100) NULL, product_id varchar(100) NULL, product_code varchar(100) NULL, expiration_date date NULL, alarm_date date NULL, description text NULL, seq int4 DEFAULT nextval('salesforce2.expirationdate__mng_id_seq'::regclass) NOT NULL, inst_dt timestamp NULL, updt_dt timestamp NULL, CONSTRAINT expirationdate__mng_pkey PRIMARY KEY (seq));
COMMENT ON TABLE salesforce2.expirationdate__mng IS '판매사원_유통기한_알림관리 테이블';

-- Permissions

ALTER TABLE salesforce2.expirationdate__mng OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.expirationdate__mng TO u4bee3ek26k44g;


-- salesforce2.hqreview__c definition

-- Drop table

-- DROP TABLE salesforce2.hqreview__c;

CREATE TABLE salesforce2.hqreview__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, branchcode__c varchar(100) NULL, branchname__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, firstdayofmonth__c date NULL, evaluationytype__c varchar(255) NULL, abctypecode__c varchar(255) NULL, hr_code_c__c varchar(255) NULL, CONSTRAINT hqreview__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_hqreview__c_systemmodstamp ON salesforce2.hqreview__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_hqreview__c_sfid ON salesforce2.hqreview__c USING btree (sfid);

-- Table Triggers

create trigger hc_hqreview__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.hqreview__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_hqreview__c_logger();
create trigger hc_hqreview__c_status_trigger before
insert
    or
update
    on
    salesforce2.hqreview__c for each row execute function salesforce2.hc_hqreview__c_status();

-- Permissions

ALTER TABLE salesforce2.hqreview__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.hqreview__c TO u4bee3ek26k44g;


-- salesforce2.if_product__c definition

-- Drop table

-- DROP TABLE salesforce2.if_product__c;

CREATE TABLE salesforce2.if_product__c ( dkretail__orderingunit__c varchar(40) NULL, dkretail__shelflifeunit__c varchar(40) NULL, dkretail__producttype__c varchar(255) NULL, dkretail__standardunitprice__c float8 NULL, dkretail__categorycode3__c varchar(100) NULL, dkretail__categorycode2__c varchar(100) NULL, dkretail__shelflife__c varchar(30) NULL, dkretail__categorycode1__c varchar(100) NULL, "name" varchar(80) NULL, dkretail__logisticsbarcode__c varchar(100) NULL, dkretail__productstatus__c varchar(255) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, dkretail__productcode__c varchar(100) NULL, dkretail__unit__c varchar(40) NULL, createddate timestamp NULL, dkretail__category3__c varchar(255) NULL, dkretail__launchdate__c date NULL, dkretail__conversionquantity__c float8 NULL, dkretail__boxreceivingquantity__c float8 NULL, dkretail__category2__c varchar(255) NULL, dkretail__category1__c varchar(255) NULL, dkretail__storecondition__c varchar(255) NULL, sfid varchar(18) NULL, id int4 NOT NULL, supertax__c float8 NULL, tastegift__c varchar(1) NULL, allergen__c text NULL, sellingpoint__c text NULL, purpose__c text NULL, imgrefpathtxt__c text NULL, updateflag__c bool NULL, imgrefpath__c varchar(255) NULL, crosscontamination__c text NULL, productfeatures__c text NULL, imgrefpath_front__c text NULL, imgrefpath_back__c text NULL, targetaccounttype__c text NULL, CONSTRAINT if_product__c_pkey PRIMARY KEY (id));

-- Permissions

ALTER TABLE salesforce2.if_product__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.if_product__c TO u4bee3ek26k44g;


-- salesforce2.monthlysaleshistory__c definition

-- Drop table

-- DROP TABLE salesforce2.monthlysaleshistory__c;

CREATE TABLE salesforce2.monthlysaleshistory__c ( account_externalkey__c varchar(1300) NULL, lastmonthtargetfomula__c float8 NULL, account_branchname__c varchar(1300) NULL, lastmonthtargetachievedratio__c float8 NULL, fridgepurpose__c float8 NULL, "name" varchar(80) NULL, fm_year__c float8 NULL, salesmonth__c varchar(255) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, shipclosingamount__c float8 NULL, createddate timestamp NULL, lastmonthresults__c float8 NULL, account_type__c varchar(1300) NULL, salesyear__c varchar(255) NULL, abcclosingamount3__c float8 NULL, fm_month__c float8 NULL, targetmonthresults__c float8 NULL, abcclosingamount2__c float8 NULL, abcclosingamount1__c float8 NULL, ambientpurpose__c float8 NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT monthlysaleshistory__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_monthlysaleshistory__c_systemmodstamp ON salesforce2.monthlysaleshistory__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_monthlysaleshistory__c_sfid ON salesforce2.monthlysaleshistory__c USING btree (sfid);

-- Table Triggers

create trigger hc_monthlysaleshistory__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.monthlysaleshistory__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_monthlysaleshistory__c_logger();
create trigger hc_monthlysaleshistory__c_status_trigger before
insert
    or
update
    on
    salesforce2.monthlysaleshistory__c for each row execute function salesforce2.hc_monthlysaleshistory__c_status();

-- Permissions

ALTER TABLE salesforce2.monthlysaleshistory__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.monthlysaleshistory__c TO u4bee3ek26k44g;


-- salesforce2.product_favorites definition

-- Drop table

-- DROP TABLE salesforce2.product_favorites;

CREATE TABLE salesforce2.product_favorites ( employeecode varchar(80) NULL, productcode varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL);
COMMENT ON TABLE salesforce2.product_favorites IS '즐겨찾기';

-- Permissions

ALTER TABLE salesforce2.product_favorites OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.product_favorites TO u4bee3ek26k44g;


-- salesforce2.productbarcode__c definition

-- Drop table

-- DROP TABLE salesforce2.productbarcode__c;

CREATE TABLE salesforce2.productbarcode__c ( productname__c varchar(255) NULL, "name" varchar(80) NULL, isdeleted bool NULL, productunit__c varchar(255) NULL, systemmodstamp timestamp NULL, productbarcode__c varchar(255) NULL, productsequence__c varchar(255) NULL, product__c varchar(18) NULL, createddate timestamp NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT productbarcode__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_productbarcode__c_systemmodstamp ON salesforce2.productbarcode__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_productbarcode__c_sfid ON salesforce2.productbarcode__c USING btree (sfid);

-- Table Triggers

create trigger hc_productbarcode__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.productbarcode__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_productbarcode__c_logger();
create trigger hc_productbarcode__c_status_trigger before
insert
    or
update
    on
    salesforce2.productbarcode__c for each row execute function salesforce2.hc_productbarcode__c_status();

-- Permissions

ALTER TABLE salesforce2.productbarcode__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.productbarcode__c TO u4bee3ek26k44g;


-- salesforce2.pushmessage__c definition

-- Drop table

-- DROP TABLE salesforce2.pushmessage__c;

CREATE TABLE salesforce2.pushmessage__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, message__c varchar(500) NULL, scheduledate__c timestamp NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT pushmessage__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_pushmessage__c_systemmodstamp ON salesforce2.pushmessage__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_pushmessage__c_sfid ON salesforce2.pushmessage__c USING btree (sfid);

-- Table Triggers

create trigger hc_pushmessage__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.pushmessage__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_pushmessage__c_logger();
create trigger hc_pushmessage__c_status_trigger before
insert
    or
update
    on
    salesforce2.pushmessage__c for each row execute function salesforce2.hc_pushmessage__c_status();

-- Permissions

ALTER TABLE salesforce2.pushmessage__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.pushmessage__c TO u4bee3ek26k44g;


-- salesforce2.pushmessagereceiver__c definition

-- Drop table

-- DROP TABLE salesforce2.pushmessagereceiver__c;

CREATE TABLE salesforce2.pushmessagereceiver__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, employeeid__c varchar(18) NULL, messageid__c varchar(18) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT pushmessagereceiver__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_pushmessagereceiver__c_systemmodstamp ON salesforce2.pushmessagereceiver__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_pushmessagereceiver__c_sfid ON salesforce2.pushmessagereceiver__c USING btree (sfid);

-- Table Triggers

create trigger hc_pushmessagereceiver__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.pushmessagereceiver__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_pushmessagereceiver__c_logger();
create trigger hc_pushmessagereceiver__c_status_trigger before
insert
    or
update
    on
    salesforce2.pushmessagereceiver__c for each row execute function salesforce2.hc_pushmessagereceiver__c_status();

-- Permissions

ALTER TABLE salesforce2.pushmessagereceiver__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.pushmessagereceiver__c TO u4bee3ek26k44g;


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

-- Permissions

ALTER TABLE salesforce2.safetycheck__workschedule__member OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.safetycheck__workschedule__member TO u4bee3ek26k44g;


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

-- Permissions

ALTER TABLE salesforce2.safetycheck_list OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.safetycheck_list TO u4bee3ek26k44g;


-- salesforce2.staffreview__c definition

-- Drop table

-- DROP TABLE salesforce2.staffreview__c;

CREATE TABLE salesforce2.staffreview__c ( branch__c varchar(1300) NULL, dkretail_employeeid__c varchar(18) NULL, instructionsdefault__c float8 NULL, employeetotalscore__c float8 NULL, branchreviews__c varchar(18) NULL, employeename__c varchar(1300) NULL, "name" varchar(80) NULL, priority_eventitemmanage__c float8 NULL, employeenumber__c varchar(1300) NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, clothessatellite__c float8 NULL, productmanagecallment__c float8 NULL, educationalevaluation__c float8 NULL, costcentercode__c varchar(1300) NULL, displaymanageeventgoals__c float8 NULL, createddate timestamp NULL, businesspartnerties__c float8 NULL, attendance__c float8 NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT staffreview__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_staffreview__c_systemmodstamp ON salesforce2.staffreview__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_staffreview__c_sfid ON salesforce2.staffreview__c USING btree (sfid);

-- Table Triggers

create trigger hc_staffreview__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.staffreview__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_staffreview__c_logger();
create trigger hc_staffreview__c_status_trigger before
insert
    or
update
    on
    salesforce2.staffreview__c for each row execute function salesforce2.hc_staffreview__c_status();

-- Permissions

ALTER TABLE salesforce2.staffreview__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.staffreview__c TO u4bee3ek26k44g;


-- salesforce2.theme__c definition

-- Drop table

-- DROP TABLE salesforce2.theme__c;

CREATE TABLE salesforce2.theme__c ( "name" varchar(80) NULL, title__c varchar(250) NULL, publicflag__c bool NULL, isdeleted bool NULL, systemmodstamp timestamp NULL, department__c varchar(100) NULL, startdate__c date NULL, createddate timestamp NULL, enddate__c date NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, branchcode__c varchar(30) NULL, CONSTRAINT theme__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_theme__c_systemmodstamp ON salesforce2.theme__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_theme__c_sfid ON salesforce2.theme__c USING btree (sfid);

-- Table Triggers

create trigger hc_theme__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.theme__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_theme__c_logger();
create trigger hc_theme__c_status_trigger before
insert
    or
update
    on
    salesforce2.theme__c for each row execute function salesforce2.hc_theme__c_status();

-- Permissions

ALTER TABLE salesforce2.theme__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.theme__c TO u4bee3ek26k44g;


-- salesforce2.tmp_claim definition

-- Drop table

-- DROP TABLE salesforce2.tmp_claim;

CREATE TABLE salesforce2.tmp_claim ( tmp_sapaccountname varchar(80) NULL, tmp_sapaccountcode varchar(80) NULL, tmp_productname varchar(100) NULL, tmp_productcode varchar(80) NULL, tmp_expirationdate varchar(80) NULL, tmp_claimtype1 varchar(80) NULL, tmp_claimtype2 varchar(80) NULL, tmp_description text NULL, tmp_quantity varchar(80) NULL, tmp_claimimagefilename varchar(200) NULL, tmp_partimagefilename varchar(200) NULL, tmp_amount varchar(80) NULL, tmp_purchasemethod varchar(80) NULL, tmp_receiptimagefilename varchar(200) NULL, tmp_requesttype text NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_employeecode varchar(80) NULL, tmp_claimtype1_name varchar(80) NULL, tmp_claimtype2_name varchar(80) NULL, tmp_purchasecode varchar(80) NULL, tmp_claimimageextension varchar(80) NULL, tmp_partimageextension varchar(80) NULL, tmp_receiptimageextension varchar(80) NULL, tmp_receiptimagebuffer text NULL, tmp_partimagebuffer text NULL, tmp_claimimagebuffer text NULL, tmp_manufacturingdate varchar(80) NULL);
COMMENT ON TABLE salesforce2.tmp_claim IS '클레임 임시저장 테이블';

-- Permissions

ALTER TABLE salesforce2.tmp_claim OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_claim TO u4bee3ek26k44g;


-- salesforce2.tmp_claimcode definition

-- Drop table

-- DROP TABLE salesforce2.tmp_claimcode;

CREATE TABLE salesforce2.tmp_claimcode ( claim1_code varchar(80) NULL, claim1_name varchar(80) NULL, claim2_code varchar(80) NULL, claim2_name varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL);
COMMENT ON TABLE salesforce2.tmp_claimcode IS '클레임 카테고리 코드';

-- Permissions

ALTER TABLE salesforce2.tmp_claimcode OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_claimcode TO u4bee3ek26k44g;


-- salesforce2.tmp_onsite definition

-- Drop table

-- DROP TABLE salesforce2.tmp_onsite;

CREATE TABLE salesforce2.tmp_onsite ( tmp_employeecode varchar(80) NULL, tmp_themecode varchar(80) NULL, tmp_themename varchar(80) NULL, tmp_classification varchar(80) NULL, tmp_sapaccountname varchar(100) NULL, tmp_sapaccoutncode varchar(80) NULL, tmp_category varchar(100) NULL, tmp_activitydate varchar(80) NULL, tmp_description text NULL, tmp_productname varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_competitorname varchar(80) NULL, tmp_competitoractivity text NULL, tmp_sampletastflag bpchar(1) NULL, tmp_competitorproudctname varchar(80) NULL, tmp_sampletasterprice varchar(80) NULL, tmp_activityamount varchar(40) NULL, tmp_s3imagekey1 varchar(255) NULL, tmp_s3imagekey2 varchar(255) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_s3imagefilename1 varchar(80) NULL, tmp_s3imagefilesize1 varchar(80) NULL, tmp_s3imagefilename2 varchar(80) NULL, tmp_s3imagefilesize2 varchar(80) NULL);

-- Permissions

ALTER TABLE salesforce2.tmp_onsite OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_onsite TO u4bee3ek26k44g;


-- salesforce2.tmp_order definition

-- Drop table

-- DROP TABLE salesforce2.tmp_order;

CREATE TABLE salesforce2.tmp_order ( tmp_employeecode varchar(80) NOT NULL, tmp_accountcode varchar(80) NULL, tmp_orderdate date NULL, tmp_totalamount varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, CONSTRAINT tmp_order_pkey PRIMARY KEY (tmp_employeecode));
COMMENT ON TABLE salesforce2.tmp_order IS '주문서 작성 임시저장 테이블';

-- Permissions

ALTER TABLE salesforce2.tmp_order OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_order TO u4bee3ek26k44g;


-- salesforce2.tmp_order_product definition

-- Drop table

-- DROP TABLE salesforce2.tmp_order_product;

CREATE TABLE salesforce2.tmp_order_product ( tmp_employeecode varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_boxcnt varchar(80) NULL, tmp_eacnt varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_totalcnt varchar(80) NULL);
COMMENT ON TABLE salesforce2.tmp_order_product IS '주문서작성 제품 임시저장 테이블';

-- Permissions

ALTER TABLE salesforce2.tmp_order_product OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_order_product TO u4bee3ek26k44g;


-- salesforce2.tmp_promotion definition

-- Drop table

-- DROP TABLE salesforce2.tmp_promotion;

CREATE TABLE salesforce2.tmp_promotion ( tmp_employeecode varchar(80) NULL, tmp_promotiontype varchar(80) NULL, tmp_promotionname varchar(100) NULL, tmp_promotionproductname varchar(80) NULL, tmp_promotionproductcode varchar(80) NULL, tmp_baseprice varchar(80) NULL, tmp_primaryquantity varchar(80) NULL, tmp_otherproduct varchar(80) NULL, tmp_otherquantity varchar(80) NULL, tmp_othertotalamount varchar(80) NULL, tmp_imageurl varchar(200) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_promotion_id varchar(80) NULL, tmp_promotion_seq varchar(80) NULL, tmp_imagefilename varchar(80) NULL, tmp_otherchangeproduct varchar(80) NULL);

-- Permissions

ALTER TABLE salesforce2.tmp_promotion OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_promotion TO u4bee3ek26k44g;


-- salesforce2.tmp_suggest definition

-- Drop table

-- DROP TABLE salesforce2.tmp_suggest;

CREATE TABLE salesforce2.tmp_suggest ( tmp_category varchar(80) NULL, tmp_productname varchar(80) NULL, tmp_productcode varchar(80) NULL, tmp_title varchar(100) NULL, tmp_description text NULL, tmp_employeecode varchar(80) NULL, tmp_s3imageurl1 varchar(80) NULL, tmp_s3imageurl2 varchar(80) NULL, inst_date timestamp NULL, upd_date timestamp NULL, tmp_s3imagefilename1 varchar(80) NULL, tmp_s3imagefilename2 varchar(80) NULL, tmp_s3imagefilesize1 varchar(80) NULL, tmp_s3imagefilesize2 varchar(80) NULL, tmp_carnumber text NULL, tmp_accountcode text NULL, tmp_claimlist text NULL, tmp_claimdate date NULL);
COMMENT ON TABLE salesforce2.tmp_suggest IS '제안하기 임시저장 테이블';

-- Permissions

ALTER TABLE salesforce2.tmp_suggest OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.tmp_suggest TO u4bee3ek26k44g;


-- salesforce2.uploadfile__c definition

-- Drop table

-- DROP TABLE salesforce2.uploadfile__c;

CREATE TABLE salesforce2.uploadfile__c ( createddate timestamp NULL, isdeleted bool NULL, "name" varchar(80) NULL, systemmodstamp timestamp NULL, recordid__c varchar(40) NULL, uniquekey__c varchar(100) NULL, size__c varchar(100) NULL, sfid varchar(18) NULL, id serial4 NOT NULL, _hc_lastop varchar(32) NULL, _hc_err text NULL, CONSTRAINT uploadfile__c_pkey PRIMARY KEY (id));
CREATE INDEX hc_idx_uploadfile__c_systemmodstamp ON salesforce2.uploadfile__c USING btree (systemmodstamp);
CREATE UNIQUE INDEX hcu_idx_uploadfile__c_sfid ON salesforce2.uploadfile__c USING btree (sfid);

-- Table Triggers

create trigger hc_uploadfile__c_logtrigger after
insert
    or
delete
    or
update
    on
    salesforce2.uploadfile__c for each row
    when (((get_xmlbinary())::text = 'base64'::text)) execute function salesforce2.hc_uploadfile__c_logger();
create trigger hc_uploadfile__c_status_trigger before
insert
    or
update
    on
    salesforce2.uploadfile__c for each row execute function salesforce2.hc_uploadfile__c_status();

-- Permissions

ALTER TABLE salesforce2.uploadfile__c OWNER TO u4bee3ek26k44g;
GRANT ALL ON TABLE salesforce2.uploadfile__c TO u4bee3ek26k44g;



-- DROP FUNCTION salesforce2.get_xmlbinary();

CREATE OR REPLACE FUNCTION salesforce2.get_xmlbinary()
 RETURNS character varying
 LANGUAGE plpgsql
AS $function$                    DECLARE
                      xmlbin varchar;
                    BEGIN
                      select into xmlbin setting from pg_settings where name='xmlbinary';
                      RETURN xmlbin;
                    END;
                 $function$
;

COMMENT ON FUNCTION salesforce2.get_xmlbinary() IS 'get_xmlbinary 함수 없을 경우 
heroku_connect로 관리되는 테이블에 crud 불가능';

-- Permissions

ALTER FUNCTION salesforce2.get_xmlbinary() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.get_xmlbinary() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_account_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_account_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_account_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_account_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_account_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_account_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_account_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_account_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_agreementword__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_agreementword__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_agreementword__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_agreementword__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_agreementword__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_agreementword__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_agreementword__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_agreementword__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_capture_insert_from_row(hstore, varchar, _text);

CREATE OR REPLACE FUNCTION salesforce2.hc_capture_insert_from_row(source_row hstore, table_name character varying, excluded_cols text[] DEFAULT ARRAY[]::text[])
 RETURNS integer
 LANGUAGE plpgsql
AS $function$
        DECLARE
            excluded_cols_standard text[] = ARRAY['_hc_lastop', '_hc_err']::text[];
            retval int;

        BEGIN
            -- VERSION 1 --

            IF (source_row -> 'id') IS NULL THEN
                -- source_row is required to have an int id value
                RETURN NULL;
            END IF;

            excluded_cols_standard := array_remove(
                array_remove(excluded_cols, 'id'), 'sfid') || excluded_cols_standard;
            INSERT INTO "salesforce2"."_trigger_log" (
                action, table_name, txid, created_at, state, record_id, values)
            VALUES (
                'INSERT', table_name, txid_current(), clock_timestamp(), 'NEW',
                (source_row -> 'id')::int,
                source_row - excluded_cols_standard
            ) RETURNING id INTO retval;
            RETURN retval;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_capture_insert_from_row(hstore, varchar, _text) OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_capture_insert_from_row(hstore, varchar, _text) TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_capture_update_from_row(hstore, varchar, _text);

CREATE OR REPLACE FUNCTION salesforce2.hc_capture_update_from_row(source_row hstore, table_name character varying, columns_to_include text[] DEFAULT ARRAY[]::text[])
 RETURNS integer
 LANGUAGE plpgsql
AS $function$
        DECLARE
            excluded_cols_standard text[] = ARRAY['_hc_lastop', '_hc_err']::text[];
            excluded_cols text[];
            retval int;

        BEGIN
            -- VERSION 1 --

            IF (source_row -> 'id') IS NULL THEN
                -- source_row is required to have an int id value
                RETURN NULL;
            END IF;

            IF array_length(columns_to_include, 1) <> 0 THEN
                excluded_cols := array(
                    select skeys(source_row)
                    except
                    select unnest(columns_to_include)
                );
            END IF;
            excluded_cols_standard := excluded_cols || excluded_cols_standard;
            INSERT INTO "salesforce2"."_trigger_log" (
                action, table_name, txid, created_at, state, record_id, sfid, values, old)
            VALUES (
                'UPDATE', table_name, txid_current(), clock_timestamp(), 'NEW',
                (source_row -> 'id')::int, source_row -> 'sfid',
                source_row - excluded_cols_standard, NULL
            ) RETURNING id INTO retval;
            RETURN retval;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_capture_update_from_row(hstore, varchar, _text) OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_capture_update_from_row(hstore, varchar, _text) TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_displayworkschedulemaster__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_displayworkschedulemaster__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_displayworkschedulemaster__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_displayworkschedulemaster__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_displayworkschedulemaster__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_displayworkschedulemaster__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_displayworkschedulemaster__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_displayworkschedulemaster__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__employee__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__employee__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__employee__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__employee__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__employee__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__employee__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__employee__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__employee__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__notice__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__notice__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__notice__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__notice__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__notice__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__notice__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__notice__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__notice__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__product__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__product__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__product__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__product__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__product__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__product__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__product__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__product__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'NEW';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                ELSE
                    trigger_row.state = 'IGNORE';
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (trigger_row.values = '') THEN
                    -- nothing changed, ignore
                    trigger_row.state = 'IGNORE';
                END IF;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_dkretail__teammemberschedule__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_hqreview__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_hqreview__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_hqreview__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_hqreview__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_hqreview__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_hqreview__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_hqreview__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_hqreview__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_monthlysaleshistory__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_monthlysaleshistory__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_monthlysaleshistory__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_monthlysaleshistory__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_monthlysaleshistory__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_monthlysaleshistory__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_monthlysaleshistory__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_monthlysaleshistory__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_productbarcode__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_productbarcode__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_productbarcode__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_productbarcode__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_productbarcode__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_productbarcode__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_productbarcode__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_productbarcode__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_pushmessage__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_pushmessage__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_pushmessage__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_pushmessage__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_pushmessage__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_pushmessage__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_pushmessage__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_pushmessage__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_pushmessagereceiver__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_pushmessagereceiver__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_pushmessagereceiver__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_pushmessagereceiver__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_pushmessagereceiver__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_pushmessagereceiver__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_pushmessagereceiver__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_pushmessagereceiver__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_staffreview__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_staffreview__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_staffreview__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_staffreview__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_staffreview__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_staffreview__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_staffreview__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_staffreview__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_theme__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_theme__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_theme__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_theme__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_theme__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_theme__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_theme__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_theme__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_uploadfile__c_logger();

CREATE OR REPLACE FUNCTION salesforce2.hc_uploadfile__c_logger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

        DECLARE
            trigger_row "salesforce2"."_trigger_log";
            excluded_cols text[] = ARRAY['_hc_lastop', '_hc_err']::text[];

        BEGIN
            -- VERSION 4 --
            trigger_row = ROW();
            trigger_row.id = nextval('"salesforce2"."_trigger_log_id_seq"');
            trigger_row.action = TG_OP::text;
            trigger_row.table_name = TG_TABLE_NAME::text;
            trigger_row.txid = txid_current();
            trigger_row.created_at = clock_timestamp();
            trigger_row.state = 'READONLY';

            IF (TG_OP = 'DELETE') THEN
                trigger_row.record_id = OLD.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                
                END IF;
            ELSEIF (TG_OP = 'INSERT') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.values = hstore(NEW.*) - excluded_cols;
            ELSEIF (TG_OP = 'UPDATE') THEN
                trigger_row.record_id = NEW.id;
                trigger_row.old = hstore(OLD.*) - excluded_cols;
                trigger_row.values = (hstore(NEW.*) - hstore(trigger_row.old)) - excluded_cols;
                
                IF (OLD.sfid IS NOT NULL) THEN
                    trigger_row.sfid = OLD.sfid;
                END IF;
            END IF;

            INSERT INTO "salesforce2"."_trigger_log" VALUES (trigger_row.*);

            RETURN NULL;
        END;
        $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_uploadfile__c_logger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_uploadfile__c_logger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.hc_uploadfile__c_status();

CREATE OR REPLACE FUNCTION salesforce2.hc_uploadfile__c_status()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
                    BEGIN
                      IF (get_xmlbinary() = 'base64') THEN  -- user op
                        NEW._hc_lastop = 'PENDING';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      ELSE  -- connect op
                        IF (TG_OP = 'UPDATE' AND NEW._hc_lastop IS NOT NULL AND NEW._hc_lastop != OLD._hc_lastop) THEN
                            RETURN NEW;
                        END IF;

                        NEW._hc_lastop = 'SYNCED';
                        NEW._hc_err = NULL;
                        RETURN NEW;
                      END IF;
                    END;
                 $function$
;

-- Permissions

ALTER FUNCTION salesforce2.hc_uploadfile__c_status() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.hc_uploadfile__c_status() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.tlog_notify_trigger();

CREATE OR REPLACE FUNCTION salesforce2.tlog_notify_trigger()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
            DECLARE
             BEGIN
               -- VERSION 1 --
               PERFORM pg_notify('salesforce2.hc_trigger_log', 'ping');
               RETURN new;
             END;
            $function$
;

-- Permissions

ALTER FUNCTION salesforce2.tlog_notify_trigger() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.tlog_notify_trigger() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.trigger_employee_data();

CREATE OR REPLACE FUNCTION salesforce2.trigger_employee_data()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE	
BEGIN     
   INSERT INTO salesforce2.employee_mng (empcode__c, emp_pwd, emp_uuid, pwd_yn, emp_token, inst_date, upd_date, gps_yn, gps_yn_date)
   VALUES ( NEW.dkretail__empcode__c, '$2a$10$eeblXuKA7HS8b9a.TBP1xu/FTrV7Hua1wfmn3V8OQkjVaR79a69o2', null, true, null, now() , now(), null, null);   
   return NULL;
END;
$function$
;

COMMENT ON FUNCTION salesforce2.trigger_employee_data() IS '개발';

-- Permissions

ALTER FUNCTION salesforce2.trigger_employee_data() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.trigger_employee_data() TO u4bee3ek26k44g;

-- DROP FUNCTION salesforce2.trigger_product_data();

CREATE OR REPLACE FUNCTION salesforce2.trigger_product_data()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$DECLARE	
BEGIN     
   INSERT INTO salesforce2.if_product__c (dkretail__orderingunit__c, dkretail__shelflifeunit__c, dkretail__producttype__c, dkretail__standardunitprice__c, dkretail__categorycode3__c, dkretail__categorycode2__c, dkretail__shelflife__c, dkretail__categorycode1__c, name, dkretail__logisticsbarcode__c, dkretail__productstatus__c, isdeleted, systemmodstamp, dkretail__productcode__c, dkretail__unit__c, createddate, dkretail__category3__c, dkretail__launchdate__c, dkretail__conversionquantity__c, dkretail__boxreceivingquantity__c, dkretail__category2__c, dkretail__category1__c, dkretail__storecondition__c, sfid, id, supertax__c, tastegift__c, allergen__c, sellingpoint__c, purpose__c, imgrefpathtxt__c, imgrefpath__c, crosscontamination__c, productfeatures__c, imgrefpath_front__c, imgrefpath_back__c, targetaccounttype__c, updateflag__c)
   VALUES ( NEW.dkretail__orderingunit__c, NEW.dkretail__shelflifeunit__c, NEW.dkretail__producttype__c, NEW.dkretail__standardunitprice__c, NEW.dkretail__categorycode3__c, NEW.dkretail__categorycode2__c, NEW.dkretail__shelflife__c, NEW.dkretail__categorycode1__c, NEW.name, NEW.dkretail__logisticsbarcode__c, NEW.dkretail__productstatus__c, NEW.isdeleted, NEW.systemmodstamp, NEW.dkretail__productcode__c, NEW.dkretail__unit__c, NEW.createddate, NEW.dkretail__category3__c, NEW.dkretail__launchdate__c, NEW.dkretail__conversionquantity__c, NEW.dkretail__boxreceivingquantity__c, NEW.dkretail__category2__c, NEW.dkretail__category1__c, NEW.dkretail__storecondition__c, NEW.sfid, NEW.id, NEW.supertax__c, NEW.tastegift__c, NEW.allergen__c, NEW.sellingpoint__c, NEW.purpose__c, NEW.imgrefpathtxt__c, NEW.imgrefpath__c, NEW.crosscontamination__c, NEW.productfeatures__c, NEW.imgrefpath_front__c, NEW.imgrefpath_back__c, NEW.targetaccounttype__c, False);   
   return NULL;
END;$function$
;

COMMENT ON FUNCTION salesforce2.trigger_product_data() IS 'if용';

-- Permissions

ALTER FUNCTION salesforce2.trigger_product_data() OWNER TO u4bee3ek26k44g;
GRANT ALL ON FUNCTION salesforce2.trigger_product_data() TO u4bee3ek26k44g;


-- Permissions

GRANT ALL ON SCHEMA salesforce2 TO u4bee3ek26k44g;