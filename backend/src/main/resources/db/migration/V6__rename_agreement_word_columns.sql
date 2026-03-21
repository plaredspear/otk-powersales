-- Spec #351: AgreementWord PK 및 레거시 컬럼명 정리
ALTER TABLE agreement_word RENAME COLUMN id TO agreement_word_id;
ALTER TABLE agreement_word RENAME COLUMN "contents__c" TO contents;
ALTER TABLE agreement_word RENAME COLUMN "active__c" TO active;
ALTER TABLE agreement_word RENAME COLUMN "activedate__c" TO active_date;
ALTER TABLE agreement_word RENAME COLUMN "afteractivedate__c" TO after_active_date;
ALTER TABLE agreement_word RENAME COLUMN isdeleted TO is_deleted;
