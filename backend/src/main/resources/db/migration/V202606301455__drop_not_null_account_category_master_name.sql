-- SF 정합: account_category_master.name NOT NULL 해제.
--
-- SF AccountCategoryMaster__c 의 Name 필드는 describe 실측 nillable=true (object-meta nameField
-- type=Text, required 강제 없음) 라 SF 가 NULL 을 허용한다. 그러나 신규 DB 는 name 을 NOT NULL 로
-- 두어 SF NULL row 적재 + SAP inbound(IF_REST_SAP_AccountMaster) 의 무검증 raw 적재와 어긋났다.
-- 레거시 SAP inbound 는 Database.upsert(list, AccountCode__c, false) 로 Name 공란 행도 그대로
-- 적재하므로(allOrNone=false 행 격리), 신규도 명시 필수 검증을 제거하고 DB NOT NULL 을 해제하여
-- SF NULL 을 NULL 그대로 보존한다. account_code UNIQUE 제약은 SF external key 정합으로 유지.
ALTER TABLE powersales.account_category_master ALTER COLUMN name DROP NOT NULL;
