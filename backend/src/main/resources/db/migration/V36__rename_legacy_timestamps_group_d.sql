-- Spec #306: Group D 엔티티 레거시 컬럼명 → created_at/updated_at 통일
-- D1: createddate/systemmodstamp, D2: created_date/system_mod_stamp, D3: inst_date/upd_date

-- ============================================================
-- D1 그룹 (10 테이블): createddate → created_at, systemmodstamp → updated_at
-- ============================================================

-- dkretail__notice__c
ALTER TABLE dkretail__notice__c RENAME COLUMN createddate TO created_at;
ALTER TABLE dkretail__notice__c RENAME COLUMN systemmodstamp TO updated_at;

-- pushmessage__c
ALTER TABLE pushmessage__c RENAME COLUMN createddate TO created_at;
ALTER TABLE pushmessage__c RENAME COLUMN systemmodstamp TO updated_at;

-- pushmessagereceiver__c
ALTER TABLE pushmessagereceiver__c RENAME COLUMN createddate TO created_at;
ALTER TABLE pushmessagereceiver__c RENAME COLUMN systemmodstamp TO updated_at;

-- staffreview__c
ALTER TABLE staffreview__c RENAME COLUMN createddate TO created_at;
ALTER TABLE staffreview__c RENAME COLUMN systemmodstamp TO updated_at;

-- hqreview__c
ALTER TABLE hqreview__c RENAME COLUMN createddate TO created_at;
ALTER TABLE hqreview__c RENAME COLUMN systemmodstamp TO updated_at;

-- theme__c (InspectionTheme)
ALTER TABLE theme__c RENAME COLUMN createddate TO created_at;
ALTER TABLE theme__c RENAME COLUMN systemmodstamp TO updated_at;

-- monthlysaleshistory__c
ALTER TABLE monthlysaleshistory__c RENAME COLUMN createddate TO created_at;
ALTER TABLE monthlysaleshistory__c RENAME COLUMN systemmodstamp TO updated_at;

-- uploadfile__c (updated_at 없음 → 추가)
ALTER TABLE uploadfile__c RENAME COLUMN createddate TO created_at;
ALTER TABLE uploadfile__c ADD COLUMN updated_at TIMESTAMP;

-- agreementword__c (updated_at 없음 → 추가)
ALTER TABLE agreementword__c RENAME COLUMN createddate TO created_at;
ALTER TABLE agreementword__c ADD COLUMN updated_at TIMESTAMP;

-- agreementhistory__c (updated_at 없음 → 추가)
ALTER TABLE agreementhistory__c RENAME COLUMN createddate TO created_at;
ALTER TABLE agreementhistory__c ADD COLUMN updated_at TIMESTAMP;

-- ============================================================
-- D2 그룹 (5 테이블): created_date → created_at, system_mod_stamp/systemmodstamp → updated_at
-- ============================================================

-- product
ALTER TABLE product RENAME COLUMN created_date TO created_at;
ALTER TABLE product RENAME COLUMN system_mod_stamp TO updated_at;

-- employee (User)
ALTER TABLE employee RENAME COLUMN created_date TO created_at;
ALTER TABLE employee RENAME COLUMN systemmodstamp TO updated_at;

-- display_work_schedule
ALTER TABLE display_work_schedule RENAME COLUMN created_date TO created_at;
ALTER TABLE display_work_schedule RENAME COLUMN systemmodstamp TO updated_at;

-- team_member_schedule
ALTER TABLE team_member_schedule RENAME COLUMN created_date TO created_at;
ALTER TABLE team_member_schedule RENAME COLUMN systemmodstamp TO updated_at;

-- if_product (InterfaceProduct)
ALTER TABLE if_product RENAME COLUMN created_date TO created_at;
ALTER TABLE if_product RENAME COLUMN system_mod_stamp TO updated_at;

-- employee_mng (EmployeeMng)
ALTER TABLE employee_mng RENAME COLUMN inst_date TO created_at;
ALTER TABLE employee_mng RENAME COLUMN upd_date TO updated_at;

-- ============================================================
-- D3 그룹 (4 테이블): inst_date/inst_dt → created_at, upd_date/updt_dt → updated_at
-- ============================================================

-- expirationdate__mng (ShelfLife)
ALTER TABLE expirationdate__mng RENAME COLUMN inst_dt TO created_at;
ALTER TABLE expirationdate__mng RENAME COLUMN updt_dt TO updated_at;

-- product_favorites (FavoriteProduct)
ALTER TABLE product_favorites RENAME COLUMN inst_date TO created_at;
ALTER TABLE product_favorites RENAME COLUMN upd_date TO updated_at;

-- tmp_claim
ALTER TABLE tmp_claim RENAME COLUMN inst_date TO created_at;
ALTER TABLE tmp_claim RENAME COLUMN upd_date TO updated_at;

-- education_mng (EducationPost)
ALTER TABLE education_mng RENAME COLUMN inst_date TO created_at;
ALTER TABLE education_mng RENAME COLUMN upd_date TO updated_at;

-- ============================================================
-- NULL 데이터 채우기 + NOT NULL 제약조건
-- ============================================================

-- D1 그룹
UPDATE dkretail__notice__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE dkretail__notice__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE dkretail__notice__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE dkretail__notice__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE pushmessage__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE pushmessage__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE pushmessage__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE pushmessage__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE pushmessagereceiver__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE pushmessagereceiver__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE pushmessagereceiver__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE pushmessagereceiver__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE staffreview__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE staffreview__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE staffreview__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE staffreview__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE hqreview__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE hqreview__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE hqreview__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE hqreview__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE theme__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE theme__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE theme__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE theme__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE monthlysaleshistory__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE monthlysaleshistory__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE monthlysaleshistory__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE monthlysaleshistory__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE uploadfile__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE uploadfile__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE uploadfile__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE uploadfile__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE agreementword__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE agreementword__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE agreementword__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE agreementword__c ALTER COLUMN updated_at SET NOT NULL;

UPDATE agreementhistory__c SET created_at = NOW() WHERE created_at IS NULL;
UPDATE agreementhistory__c SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE agreementhistory__c ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE agreementhistory__c ALTER COLUMN updated_at SET NOT NULL;

-- D2 그룹
UPDATE product SET created_at = NOW() WHERE created_at IS NULL;
UPDATE product SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE product ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE product ALTER COLUMN updated_at SET NOT NULL;

UPDATE employee SET created_at = NOW() WHERE created_at IS NULL;
UPDATE employee SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE employee ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE employee ALTER COLUMN updated_at SET NOT NULL;

UPDATE display_work_schedule SET created_at = NOW() WHERE created_at IS NULL;
UPDATE display_work_schedule SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE display_work_schedule ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE display_work_schedule ALTER COLUMN updated_at SET NOT NULL;

UPDATE team_member_schedule SET created_at = NOW() WHERE created_at IS NULL;
UPDATE team_member_schedule SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE team_member_schedule ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE team_member_schedule ALTER COLUMN updated_at SET NOT NULL;

UPDATE if_product SET created_at = NOW() WHERE created_at IS NULL;
UPDATE if_product SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE if_product ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE if_product ALTER COLUMN updated_at SET NOT NULL;

UPDATE employee_mng SET created_at = NOW() WHERE created_at IS NULL;
UPDATE employee_mng SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE employee_mng ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE employee_mng ALTER COLUMN updated_at SET NOT NULL;

-- D3 그룹
UPDATE expirationdate__mng SET created_at = NOW() WHERE created_at IS NULL;
UPDATE expirationdate__mng SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE expirationdate__mng ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE expirationdate__mng ALTER COLUMN updated_at SET NOT NULL;

UPDATE product_favorites SET created_at = NOW() WHERE created_at IS NULL;
UPDATE product_favorites SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE product_favorites ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE product_favorites ALTER COLUMN updated_at SET NOT NULL;

UPDATE tmp_claim SET created_at = NOW() WHERE created_at IS NULL;
UPDATE tmp_claim SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE tmp_claim ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE tmp_claim ALTER COLUMN updated_at SET NOT NULL;

UPDATE education_mng SET created_at = NOW() WHERE created_at IS NULL;
UPDATE education_mng SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE education_mng ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE education_mng ALTER COLUMN updated_at SET NOT NULL;
