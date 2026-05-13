-- Spec #740 Track A: SF Formula 필드 22건 정합 제거
-- README §6.7 정책: Formula 필드는 DB 컬럼 추가 X + @SFField 미부여
-- 사용자 결정 (2026-05-13): sobject 기준 정합. 22건 모두 entity 매핑 + DB 컬럼 완전 제거.
-- 추후 application 운영 요건으로 컬럼이 다시 필요해지면 SF 무관 일반 컬럼으로 재도입.

-- AlternativeHoliday (1)
ALTER TABLE alternative_holiday DROP COLUMN IF EXISTS employee_name;

-- Product (1)
ALTER TABLE product DROP COLUMN IF EXISTS shelf_life_full;

-- Promotion (8)
ALTER TABLE promotion DROP COLUMN IF EXISTS promotion_name;
ALTER TABLE promotion DROP COLUMN IF EXISTS target_amount;
ALTER TABLE promotion DROP COLUMN IF EXISTS actual_amount;
ALTER TABLE promotion DROP COLUMN IF EXISTS branch_name;
ALTER TABLE promotion DROP COLUMN IF EXISTS category;
ALTER TABLE promotion DROP COLUMN IF EXISTS account_code;
ALTER TABLE promotion DROP COLUMN IF EXISTS actual_amount_won;
ALTER TABLE promotion DROP COLUMN IF EXISTS product_code;

-- PromotionEmployee (2)
ALTER TABLE promotion_employee DROP COLUMN IF EXISTS work_type4;
ALTER TABLE promotion_employee DROP COLUMN IF EXISTS professional_promotion_team;

-- ProfessionalPromotionTeamMaster (2)
ALTER TABLE professional_promotion_team_master DROP COLUMN IF EXISTS employee_number;
ALTER TABLE professional_promotion_team_master DROP COLUMN IF EXISTS branch_name;

-- MonthlySalesHistory (8)
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS account_external_key;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS account_branch_name;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS account_type;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS fm_year;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS fm_month;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS target_month_results;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS last_month_target_formula;
ALTER TABLE monthly_sales_history DROP COLUMN IF EXISTS last_month_target_achieved_ratio;
