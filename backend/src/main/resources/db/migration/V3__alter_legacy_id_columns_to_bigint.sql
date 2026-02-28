-- 레거시 __c 테이블의 id 컬럼을 serial(integer) → bigint로 변경
-- Hibernate 엔티티가 Long(bigint) 타입을 사용하므로 스키마 일치 필요

ALTER TABLE salesforce2.agreementword__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.displayworkschedulemaster__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.dkretail__employee__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.dkretail__notice__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.dkretail__product__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.dkretail__teammemberschedule__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.hqreview__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.if_product__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.monthlysaleshistory__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.productbarcode__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.pushmessage__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.pushmessagereceiver__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.staffreview__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.theme__c ALTER COLUMN id TYPE bigint;
ALTER TABLE salesforce2.uploadfile__c ALTER COLUMN id TYPE bigint;
