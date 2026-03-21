ALTER TABLE agreement_history RENAME COLUMN id TO agreement_history_id;
ALTER TABLE agreement_history RENAME COLUMN "employeeid__c" TO employee_id;
ALTER TABLE agreement_history RENAME COLUMN "agreementflag__c" TO agreement_flag;
ALTER TABLE agreement_history RENAME COLUMN "agreementdate__c" TO agreement_date;
ALTER TABLE agreement_history RENAME COLUMN "agreementwordid__c" TO agreement_word_id;
ALTER TABLE agreement_history RENAME COLUMN isdeleted TO is_deleted;
