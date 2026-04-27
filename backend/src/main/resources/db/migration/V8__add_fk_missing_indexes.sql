-- 스펙 #547: FK 누락 인덱스 일괄 추가 (20건)
--
-- PostgreSQL 은 FOREIGN KEY 제약을 추가해도 child 측 FK 컬럼에 자동으로 인덱스를 만들지
-- 않는다 (PRIMARY KEY / UNIQUE 와 다른 점). 누락 시 다음 상황에서 성능 저하가 발생한다.
--   1. parent 의 DELETE/UPDATE 시 child 전체 Seq Scan (RI 검증)
--   2. child → parent 조인 / WHERE 필터
-- V1~V5 시점의 29개 FK 중 인덱스로 커버되지 않는 20개를 본 마이그레이션이 일괄 추가한다.
--
-- 명명 규칙: idx_<table>_<column>. 단 `monthly_female_employee_integration_schedule` 는
-- 식별자 한도(63 byte) 가독성을 위해 `mfei_schedule` 로 축약 (인덱스명 한정).
-- staff_review.employee_id 는 #546 스펙에서 별도 추가하므로 본 스펙 범위 외.

CREATE INDEX idx_claim_photos_claim_id ON powersales.claim_photos (claim_id);
CREATE INDEX idx_claim_subcategories_category_id ON powersales.claim_subcategories (category_id);
CREATE INDEX idx_claim_category_id ON powersales.claim (category_id);
CREATE INDEX idx_claim_subcategory_id ON powersales.claim (subcategory_id);
CREATE INDEX idx_agreement_history_agreement_word_id ON powersales.agreement_history (agreement_word_id);
CREATE INDEX idx_agreement_history_employee_id ON powersales.agreement_history (employee_id);
CREATE INDEX idx_alternative_holiday_employee_id ON powersales.alternative_holiday (employee_id);
CREATE INDEX idx_education_post_attachment_education_post_id ON powersales.education_post_attachment (education_post_id);
CREATE INDEX idx_attendance_log_account_id ON powersales.attendance_log (account_id);
CREATE INDEX idx_attendance_log_employee_id ON powersales.attendance_log (employee_id);
CREATE INDEX idx_education_post_employee_id ON powersales.education_post (employee_id);
CREATE INDEX idx_erp_order_product_erp_order_id ON powersales.erp_order_product (erp_order_id);
CREATE INDEX idx_login_history_employee_code ON powersales.login_history (employee_code);
CREATE INDEX idx_product_barcode_product_id ON powersales.product_barcode (product_id);
CREATE INDEX idx_safety_check_submission_display_work_schedule_id ON powersales.safety_check_submission (display_work_schedule_id);
CREATE INDEX idx_safety_check_submission_employee_id ON powersales.safety_check_submission (employee_id);
CREATE INDEX idx_safety_check_submission_team_member_schedule_id ON powersales.safety_check_submission (team_member_schedule_id);
CREATE INDEX idx_mfei_schedule_account_id ON powersales.monthly_female_employee_integration_schedule (account_id);
CREATE INDEX idx_mfei_schedule_employee_id ON powersales.monthly_female_employee_integration_schedule (employee_id);
CREATE INDEX idx_user_permission_granted_by ON powersales.user_permission (granted_by);
