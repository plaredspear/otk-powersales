-- SF 정합: system_code_master 의 company_code / group_code / detail_code NOT NULL 해제.
--
-- SF SystemCodeMaster__c 의 CompanyCode__c / GroupCode__c / DetailCode__c 는 describe 실측
-- nillable=true (field-meta required=false) 라 SF 가 NULL 을 허용한다. 그러나 신규 DB 는 세 컬럼을
-- NOT NULL 로 두어 SAP inbound(IF_REST_SAP_SystemCodeMaster) 의 무검증 raw 적재와 어긋났다.
-- 레거시 SAP inbound 는 Database.upsert(list, ExternalKey__c, false) 로 세 코드 공란 행도 그대로
-- 적재하므로(allOrNone=false 행 격리), 신규도 명시 필수 검증을 제거하고 DB NOT NULL 을 해제하여
-- SF NULL 을 NULL 그대로 보존한다. 식별/UNIQUE 는 합성 키 external_key(NOT NULL+UNIQUE)가 담당하므로
-- external_key 의 NOT NULL 제약은 SF ExternalKey__c required=true 정합으로 유지한다.
ALTER TABLE powersales.system_code_master ALTER COLUMN company_code DROP NOT NULL;
ALTER TABLE powersales.system_code_master ALTER COLUMN group_code DROP NOT NULL;
ALTER TABLE powersales.system_code_master ALTER COLUMN detail_code DROP NOT NULL;
