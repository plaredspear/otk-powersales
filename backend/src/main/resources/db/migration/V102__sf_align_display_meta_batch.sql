-- Spec #747 STEP1 — 카테고리 A 도메인 핵심 누락 D 분류 일괄 추가.
-- 근거: docs/plan/old_source_260408/sf-object-meta/README.md §6.2 D 분류 자동 적용.
-- 분석: 36 sobject 정합 분석 2026-05-13
-- Q1 검증: myAccount/Age/yearsOfService 는 calculated=true → §6.7 Formula 제외 (entity 미추가)
-- Q4: 카테고리 B (Account/Employee 표시 redundancy 35건) 일괄 제외 — #747 deviation 박제
-- Q3: 도메인 의미 미확정 필드 (Claim.ClaimManagement, CostCenter, Notice.Department, PPTMaster.Valid* 등) 본 batch 보류

-- 1. Claim — 도메인 핵심 (사용처 무관 신규 컬럼)
ALTER TABLE claim ADD COLUMN IF NOT EXISTS barcode VARCHAR(1300);
ALTER TABLE claim ADD COLUMN IF NOT EXISTS phone VARCHAR(1300);
ALTER TABLE claim ADD COLUMN IF NOT EXISTS product_status VARCHAR(1300);

-- 2. Notice — 작성일
ALTER TABLE notice ADD COLUMN IF NOT EXISTS notice_date DATE;

-- 3. OrderRequest — 테스트용 자동번호
ALTER TABLE order_request ADD COLUMN IF NOT EXISTS request_number_test VARCHAR(30);

-- 4. ErpOrderProduct — 주문일자
ALTER TABLE erp_order_product ADD COLUMN IF NOT EXISTS order_date DATE;

-- 5. StaffReview — 구분/입사일/직위
ALTER TABLE staff_review ADD COLUMN IF NOT EXISTS employee_type VARCHAR(1300);
ALTER TABLE staff_review ADD COLUMN IF NOT EXISTS entry_date DATE;
ALTER TABLE staff_review ADD COLUMN IF NOT EXISTS jikwee VARCHAR(1300);

-- 6. UploadFile — 생성일
ALTER TABLE upload_file ADD COLUMN IF NOT EXISTS file_date DATE;

-- 7. Product — 표준출고가 + 별도 박스 수신
ALTER TABLE product ADD COLUMN IF NOT EXISTS standard_price DOUBLE PRECISION;
ALTER TABLE product ADD COLUMN IF NOT EXISTS legacy_box_receiving_quantity DOUBLE PRECISION;

-- 8. ProfessionalPromotionTeamMaster — 퇴직일
ALTER TABLE professional_promotion_team_master ADD COLUMN IF NOT EXISTS quit_date DATE;

-- 9. Promotion — 프로모션명 + ActualAmount 동명 둘 다 + TargetAmount (Q5 옵션 1)
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS promotion_name VARCHAR(1300);
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS dk_actual_amount DOUBLE PRECISION;
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS dk_target_amount DOUBLE PRECISION;
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS actual_amount DOUBLE PRECISION;

-- 10. MonthlyFemaleEmployeeIntegrationSchedule — 보고일자/연월
ALTER TABLE monthly_female_employee_integration_schedule ADD COLUMN IF NOT EXISTS date_for_report DATE;
-- Year_Month__c (SF len=1300) 본 batch 보류 — 기존 month 컬럼 H2 reserved word 충돌. #750+ follow-up

-- 11. DisplayWorkSchedule — 확인 알림
ALTER TABLE display_work_schedule ADD COLUMN IF NOT EXISTS confirmation_alert VARCHAR(1300);

-- 12. TeamMemberSchedule — 도메인 보조 6건
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS actual_work_date DATE;
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS commute_date TIMESTAMP;
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS confirm_alt_holiday_date DATE;
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS dk_day DOUBLE PRECISION;
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS reason VARCHAR(1300);
ALTER TABLE team_member_schedule ADD COLUMN IF NOT EXISTS second_work_type_text VARCHAR(1300);

-- 13. AlternativeHoliday — 사원명 캐시 (Q2 history 보존)
ALTER TABLE alternative_holiday ADD COLUMN IF NOT EXISTS emp_name VARCHAR(1300);

-- 14. ProfessionalPromotionTeamHistory — 사원코드 캐시 (Q2 history 보존)
ALTER TABLE professional_promotion_team_history ADD COLUMN IF NOT EXISTS emp_code VARCHAR(1300);
