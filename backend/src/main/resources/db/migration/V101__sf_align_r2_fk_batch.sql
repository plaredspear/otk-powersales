-- Spec #746 STEP1: Reference R-2 FK 7건 신설 (`<관계명>_sfid` + `<관계명>: <Entity>?` FK 둘 다).
-- 근거: docs/plan/old_source_260408/sf-object-meta/README.md §6.4 R-2 패턴.
-- 분석: 36 sobject 정합 분석 2026-05-13
-- 비범위: TeamMemberSchedule.commute_log_id 리네임 + AttendanceLog FK — #749 follow-up 으로 분리

-- 1. OrderRequestProduct → Product
ALTER TABLE order_request_product ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES product(product_id);
CREATE INDEX IF NOT EXISTS idx_order_request_product_product_id ON order_request_product(product_id);

-- 2. ErpOrder → Account
ALTER TABLE erp_order ADD COLUMN IF NOT EXISTS account_id BIGINT REFERENCES account(account_id);
CREATE INDEX IF NOT EXISTS idx_erp_order_account_id ON erp_order(account_id);

-- 3. PromotionEmployee → TeamMemberSchedule (기존 team_member_schedule_id 컬럼 재사용; FK 관계만 JPA 매핑)
-- 컬럼 자체는 이미 존재 — FK 제약만 추가
ALTER TABLE promotion_employee
    ADD CONSTRAINT IF NOT EXISTS fk_promotion_employee_team_member_schedule
    FOREIGN KEY (team_member_schedule_id) REFERENCES team_member_schedule(team_member_schedule_id);
CREATE INDEX IF NOT EXISTS idx_promotion_employee_team_member_schedule_id ON promotion_employee(team_member_schedule_id);

-- 4. TeamMemberSchedule → MonthlyFemaleEmployeeIntegrationSchedule
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS monthly_female_employee_integration_schedule_id BIGINT
    REFERENCES monthly_female_employee_integration_schedule(monthly_female_employee_integration_schedule_id);
CREATE INDEX IF NOT EXISTS idx_tms_mfeis_id ON team_member_schedule(monthly_female_employee_integration_schedule_id);

-- 5. MonthlyFemaleEmployeeIntegrationSchedule → EmployeeInputCriteriaMaster
ALTER TABLE monthly_female_employee_integration_schedule ADD COLUMN IF NOT EXISTS employee_input_criteria_master_id BIGINT
    REFERENCES employee_input_criteria_master(employee_input_criteria_master_id);
CREATE INDEX IF NOT EXISTS idx_mfeis_eicm_id ON monthly_female_employee_integration_schedule(employee_input_criteria_master_id);

-- 6. MonthlySalesHistory → HqReview
ALTER TABLE monthly_sales_history ADD COLUMN IF NOT EXISTS hq_review_id BIGINT REFERENCES hq_review(hq_review_id);
CREATE INDEX IF NOT EXISTS idx_msh_hq_review_id ON monthly_sales_history(hq_review_id);

-- 7. MonthlySalesHistory → MonthlySalesHistory (self-reference; cascade 없음)
ALTER TABLE monthly_sales_history ADD COLUMN IF NOT EXISTS last_monthly_sales_history_id BIGINT
    REFERENCES monthly_sales_history(monthly_sales_history_id);
CREATE INDEX IF NOT EXISTS idx_msh_last_msh_id ON monthly_sales_history(last_monthly_sales_history_id);
