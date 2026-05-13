-- SF prod 메타 부재 컬럼 제거 (diff-report.md §1.2 Top 3)
-- - order_request.request_number_test (SF RequestNumberTest__c 부재)
-- - erp_order.owner_sfid + owner_id (SF ERP_Order__c OwnerId 부재)
-- - employee.prn_flag (SF DKRetail__Employee__c prnflag__c 부재)

ALTER TABLE order_request DROP COLUMN IF EXISTS request_number_test;

ALTER TABLE erp_order DROP COLUMN IF EXISTS owner_sfid;
ALTER TABLE erp_order DROP COLUMN IF EXISTS owner_id;

ALTER TABLE employee DROP COLUMN IF EXISTS prn_flag;
