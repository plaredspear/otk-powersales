-- P0 hotfix: SF Object 정합 분석 결과 데이터 손실/오버플로우 위험 6건 즉시 정합
-- 근거: docs/plan/old_source_260408/sf-object-meta/README.md §6.6, §6.8
-- 분석 시점: 2026-05-13 — 36개 sobject 정합 분석

-- 1. Claim 도메인 — SF 절단 위험 + Int 오버플로우
-- DKRetail__Quantity__c (SF double 18,0) → entity Int → Long
ALTER TABLE claim ALTER COLUMN defect_quantity TYPE BIGINT;

-- DKRetail__Amount__c (SF double 18,0) → entity Int → Long
ALTER TABLE claim ALTER COLUMN purchase_amount TYPE BIGINT;

-- DKRetail__ProductCode__c (SF string 1300)
ALTER TABLE claim ALTER COLUMN product_code TYPE VARCHAR(1300);

-- DKRetail__Description__c (SF textarea 4000)
ALTER TABLE claim ALTER COLUMN defect_description TYPE VARCHAR(4000);

-- 2. UploadFile 도메인 — SF 절단 위험
-- RecordId__c (SF string 40)
ALTER TABLE upload_file ALTER COLUMN record_id TYPE VARCHAR(40);

-- Size__c (SF string 100)
ALTER TABLE upload_file ALTER COLUMN size TYPE VARCHAR(100);

-- 3. AccountCategoryMaster 도메인 — SF 절단 위험
-- AccountCode__c (SF string 255 unique)
ALTER TABLE account_category_master ALTER COLUMN account_code TYPE VARCHAR(255);

-- 4. MonthlySalesHistory 도메인 — SF 절단 위험
-- Externalkey__c (SF string 40 unique)
ALTER TABLE monthly_sales_history ALTER COLUMN external_key TYPE VARCHAR(40);

-- 5. Promotion 도메인 — SF 절단 위험
-- Name (SF string 80 — Account/Standard Name 길이)
ALTER TABLE promotion ALTER COLUMN promotion_number TYPE VARCHAR(80);
