-- MonthlySalesHistory 컬럼명 가독성 개선 (#412)
-- PK 컬럼명 컨벤션 적용 + __c 접미사 제거 + snake_case 정규화

-- 1. PK 컬럼명 변경
ALTER TABLE monthly_sales_history RENAME COLUMN id TO monthly_sales_history_id;

-- 2. 레거시 컬럼명 변경 (__c 제거 + snake_case 정규화)
ALTER TABLE monthly_sales_history RENAME COLUMN account_externalkey__c TO account_external_key;
ALTER TABLE monthly_sales_history RENAME COLUMN account_branchname__c TO account_branch_name;
ALTER TABLE monthly_sales_history RENAME COLUMN account_type__c TO account_type;
ALTER TABLE monthly_sales_history RENAME COLUMN salesyear__c TO sales_year;
ALTER TABLE monthly_sales_history RENAME COLUMN salesmonth__c TO sales_month;
ALTER TABLE monthly_sales_history RENAME COLUMN fm_year__c TO fm_year;
ALTER TABLE monthly_sales_history RENAME COLUMN fm_month__c TO fm_month;
ALTER TABLE monthly_sales_history RENAME COLUMN targetmonthresults__c TO target_month_results;
ALTER TABLE monthly_sales_history RENAME COLUMN lastmonthresults__c TO last_month_results;
ALTER TABLE monthly_sales_history RENAME COLUMN lastmonthtargetfomula__c TO last_month_target_formula;
ALTER TABLE monthly_sales_history RENAME COLUMN lastmonthtargetachievedratio__c TO last_month_target_achieved_ratio;
ALTER TABLE monthly_sales_history RENAME COLUMN shipclosingamount__c TO ship_closing_amount;
ALTER TABLE monthly_sales_history RENAME COLUMN abcclosingamount1__c TO abc_closing_amount1;
ALTER TABLE monthly_sales_history RENAME COLUMN abcclosingamount2__c TO abc_closing_amount2;
ALTER TABLE monthly_sales_history RENAME COLUMN abcclosingamount3__c TO abc_closing_amount3;
ALTER TABLE monthly_sales_history RENAME COLUMN ambientpurpose__c TO ambient_purpose;
ALTER TABLE monthly_sales_history RENAME COLUMN fridgepurpose__c TO fridge_purpose;
ALTER TABLE monthly_sales_history RENAME COLUMN isdeleted TO is_deleted;
ALTER TABLE monthly_sales_history RENAME COLUMN externalkey__c TO external_key;
ALTER TABLE monthly_sales_history RENAME COLUMN rlsales__c TO rl_sales;

-- 3. UNIQUE 인덱스명 변경
ALTER INDEX monthly_sales_history_externalkey__c_key RENAME TO monthly_sales_history_external_key_key;
