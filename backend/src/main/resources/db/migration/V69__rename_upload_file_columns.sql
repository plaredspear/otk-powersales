ALTER TABLE upload_file RENAME COLUMN id TO upload_file_id;
ALTER TABLE upload_file RENAME COLUMN uniquekey__c TO unique_key;
ALTER TABLE upload_file RENAME COLUMN recordid__c TO record_id;
ALTER TABLE upload_file RENAME COLUMN size__c TO size;
ALTER TABLE upload_file RENAME COLUMN isdeleted TO is_deleted;
