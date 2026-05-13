-- SF prod Account 메타 정합: IsPriorityRecord 추가 + 20개 컬럼 길이 정합
-- 기준: docs/plan/old_source_260408/sf-object-meta/prod/Account.md
-- 정책: sandbox/README.md §6 (SF prod 라이브 권위)

-- 1) IsPriorityRecord 컬럼 추가 (C-1 분류, boolean)
ALTER TABLE account ADD COLUMN is_priority_record BOOLEAN;

-- 2) 절단 위험 — SF length > entity length (13건)
ALTER TABLE account ALTER COLUMN email TYPE VARCHAR(241);
ALTER TABLE account ALTER COLUMN account_status_code TYPE VARCHAR(100);
ALTER TABLE account ALTER COLUMN business_type TYPE VARCHAR(100);
ALTER TABLE account ALTER COLUMN business_category TYPE VARCHAR(100);
ALTER TABLE account ALTER COLUMN division_name TYPE VARCHAR(250);
ALTER TABLE account ALTER COLUMN sales_dept_name TYPE VARCHAR(250);
ALTER TABLE account ALTER COLUMN werk1 TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN werk2 TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN werk3 TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN sales_dept_cost_center TYPE VARCHAR(50);
ALTER TABLE account ALTER COLUMN division_cost_center TYPE VARCHAR(50);
ALTER TABLE account ALTER COLUMN distribution TYPE VARCHAR(20);
ALTER TABLE account ALTER COLUMN consignment_acc TYPE VARCHAR(40);

-- 3) entity 과대 — SF 정합 축소 (2건)
ALTER TABLE account ALTER COLUMN business_license_number TYPE VARCHAR(20);
ALTER TABLE account ALTER COLUMN employee_code TYPE VARCHAR(15);

-- 4) Picklist 컬럼 길이 §6.6 정합 — SF picklist(255) (5건)
ALTER TABLE account ALTER COLUMN account_type TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN rating TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN ownership TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN freezer_type TYPE VARCHAR(255);
ALTER TABLE account ALTER COLUMN account_source TYPE VARCHAR(255);
