-- V47: 레거시 Salesforce __c 테이블명 → 엔티티명 일치 변경 (#311)

-- 1. 테이블 RENAME (12개)
ALTER TABLE theme__c RENAME TO inspection_theme;
ALTER TABLE dkretail__notice__c RENAME TO notice;
ALTER TABLE uploadfile__c RENAME TO upload_file;
ALTER TABLE hqreview__c RENAME TO hq_review;
ALTER TABLE staffreview__c RENAME TO staff_review;
ALTER TABLE pushmessage__c RENAME TO push_message;
ALTER TABLE agreementword__c RENAME TO agreement_word;
ALTER TABLE agreementhistory__c RENAME TO agreement_history;
ALTER TABLE monthlysaleshistory__c RENAME TO monthly_sales_history;
ALTER TABLE dkretail__promotion__c RENAME TO promotion;
ALTER TABLE dkretail__promotion_type RENAME TO promotion_type;

-- PushMessageReceiver: 엔티티가 이미 push_message_receiver로 설정됨, DB도 확인 후 조건부 RENAME
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = current_schema() AND tablename = 'pushmessagereceiver__c') THEN
    ALTER TABLE pushmessagereceiver__c RENAME TO push_message_receiver;
  END IF;
END $$;

-- 2. 시퀀스 RENAME (레거시 네이밍 → 새 테이블명 기준)
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'monthlysaleshistory__c_id_seq') THEN
    ALTER SEQUENCE monthlysaleshistory__c_id_seq RENAME TO monthly_sales_history_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'theme__c_id_seq') THEN
    ALTER SEQUENCE theme__c_id_seq RENAME TO inspection_theme_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'dkretail__notice__c_id_seq') THEN
    ALTER SEQUENCE dkretail__notice__c_id_seq RENAME TO notice_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'uploadfile__c_id_seq') THEN
    ALTER SEQUENCE uploadfile__c_id_seq RENAME TO upload_file_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'hqreview__c_id_seq') THEN
    ALTER SEQUENCE hqreview__c_id_seq RENAME TO hq_review_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'staffreview__c_id_seq') THEN
    ALTER SEQUENCE staffreview__c_id_seq RENAME TO staff_review_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'pushmessage__c_id_seq') THEN
    ALTER SEQUENCE pushmessage__c_id_seq RENAME TO push_message_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'pushmessagereceiver__c_id_seq') THEN
    ALTER SEQUENCE pushmessagereceiver__c_id_seq RENAME TO push_message_receiver_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'agreementword__c_id_seq') THEN
    ALTER SEQUENCE agreementword__c_id_seq RENAME TO agreement_word_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'agreementhistory__c_id_seq') THEN
    ALTER SEQUENCE agreementhistory__c_id_seq RENAME TO agreement_history_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'dkretail__promotion__c_id_seq') THEN
    ALTER SEQUENCE dkretail__promotion__c_id_seq RENAME TO promotion_id_seq;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = current_schema() AND sequencename = 'dkretail__promotion_type_id_seq') THEN
    ALTER SEQUENCE dkretail__promotion_type_id_seq RENAME TO promotion_type_id_seq;
  END IF;
END $$;
