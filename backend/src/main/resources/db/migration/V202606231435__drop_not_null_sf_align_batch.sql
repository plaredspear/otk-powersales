-- SF 정합: Stage1 마이그레이션 과다 필수 해소 (13개 target 32개 컬럼).
--
-- 아래 컬럼들은 SF describe 실측 nillable=true (field-meta required=false) 로 SF 가 NULL 을
-- 허용하나, DB 가 NOT NULL 로 남아 있어 SF 공란 row 가 Stage1 적재 시 "필수 필드 누락" 으로
-- 탈락한다 (OrderRequestProduct 와 동일 패턴 — V202606231430 참조). Stage1Targets.kt 의
-- nullable=false 제거와 함께 DB NOT NULL 도 해제하여 SF NULL 을 NULL 그대로 보존.
-- 신규 INSERT 경로(엔티티/DEFAULT)는 영향 없음 — 마이그레이션 경로만 NULL 보존.

-- promotion
ALTER TABLE powersales.promotion ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE powersales.promotion ALTER COLUMN end_date DROP NOT NULL;

-- account_category_master
ALTER TABLE powersales.account_category_master ALTER COLUMN account_code DROP NOT NULL;

-- agreement_history
ALTER TABLE powersales.agreement_history ALTER COLUMN agreement_date DROP NOT NULL;

-- alternative_holiday
ALTER TABLE powersales.alternative_holiday ALTER COLUMN actual_work_date DROP NOT NULL;
ALTER TABLE powersales.alternative_holiday ALTER COLUMN target_alt_holiday_date DROP NOT NULL;
ALTER TABLE powersales.alternative_holiday ALTER COLUMN status DROP NOT NULL;

-- appointment
ALTER TABLE powersales.appointment ALTER COLUMN employee_code DROP NOT NULL;
ALTER TABLE powersales.appointment ALTER COLUMN appoint_date DROP NOT NULL;

-- attend_info
ALTER TABLE powersales.attend_info ALTER COLUMN employee_code DROP NOT NULL;
ALTER TABLE powersales.attend_info ALTER COLUMN start_date DROP NOT NULL;

-- claim
ALTER TABLE powersales.claim ALTER COLUMN date DROP NOT NULL;
ALTER TABLE powersales.claim ALTER COLUMN claim_type1 DROP NOT NULL;
ALTER TABLE powersales.claim ALTER COLUMN claim_type2 DROP NOT NULL;
ALTER TABLE powersales.claim ALTER COLUMN defect_description DROP NOT NULL;
ALTER TABLE powersales.claim ALTER COLUMN defect_quantity DROP NOT NULL;
ALTER TABLE powersales.claim ALTER COLUMN status DROP NOT NULL;

-- erp_order_product
ALTER TABLE powersales.erp_order_product ALTER COLUMN sap_order_number DROP NOT NULL;
ALTER TABLE powersales.erp_order_product ALTER COLUMN line_number DROP NOT NULL;
ALTER TABLE powersales.erp_order_product ALTER COLUMN external_key DROP NOT NULL;

-- holiday_master
ALTER TABLE powersales.holiday_master ALTER COLUMN holiday_date DROP NOT NULL;
ALTER TABLE powersales.holiday_master ALTER COLUMN type DROP NOT NULL;

-- order_request
ALTER TABLE powersales.order_request ALTER COLUMN order_date DROP NOT NULL;
ALTER TABLE powersales.order_request ALTER COLUMN delivery_date DROP NOT NULL;
ALTER TABLE powersales.order_request ALTER COLUMN total_amount DROP NOT NULL;
ALTER TABLE powersales.order_request ALTER COLUMN order_request_status DROP NOT NULL;

-- professional_promotion_team_history
ALTER TABLE powersales.professional_promotion_team_history ALTER COLUMN new_value DROP NOT NULL;
ALTER TABLE powersales.professional_promotion_team_history ALTER COLUMN changed_at DROP NOT NULL;

-- professional_promotion_team_master
ALTER TABLE powersales.professional_promotion_team_master ALTER COLUMN team_type DROP NOT NULL;

-- suggestion
ALTER TABLE powersales.suggestion ALTER COLUMN title DROP NOT NULL;
ALTER TABLE powersales.suggestion ALTER COLUMN content DROP NOT NULL;
ALTER TABLE powersales.suggestion ALTER COLUMN category DROP NOT NULL;

