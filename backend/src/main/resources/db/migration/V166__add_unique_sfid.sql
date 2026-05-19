-- SF 적재 ON CONFLICT DO NOTHING 멱등성 보장 — sfid 컬럼에 UNIQUE 제약 추가.
--
-- 대상: @SFObject 가 붙은 36개 entity 중 sfid UNIQUE 가 누락된 15개 테이블.
--      나머지 21개 (User / Appointment / AttendanceLog / Group / NewProduct +
--      account / account_category_master / agreement_history / alternative_holiday /
--      attend_info / claim / employee_input_criteria_master / erp_order /
--      erp_order_product / organization / product / product_barcode /
--      professional_promotion_team_history / professional_promotion_team_master /
--      promotion / promotion_employee) 는 V1 ~ V165 의 어딘가에서 이미 적용됨.
--
-- 사유: Stage1 migrate-stage1.main.kts 의 `INSERT ... ON CONFLICT DO NOTHING` 이
--      "기존에 적재된 SF Id 는 skip" 으로 동작하려면 sfid 에 UNIQUE 제약 필요.
--      UNIQUE 없는 상태에서는 PK (auto-increment) 만 conflict 키로 사용되어
--      같은 sfid 가 중복 INSERT 됨.
--
-- NULL 처리: PostgreSQL UNIQUE 는 NULL 을 여러 번 허용 (NULL ≠ NULL).
--          신규 시스템에서 자체 생성된 row (sfid IS NULL) 는 제약 영향 없음.

ALTER TABLE powersales.agreement_word                                  ADD CONSTRAINT uk_agreement_word_sfid                                  UNIQUE (sfid);
ALTER TABLE powersales.display_work_schedule                           ADD CONSTRAINT uk_display_work_schedule_sfid                           UNIQUE (sfid);
ALTER TABLE powersales.employee                                        ADD CONSTRAINT uk_employee_sfid                                        UNIQUE (sfid);
ALTER TABLE powersales.holiday_master                                  ADD CONSTRAINT uk_holiday_master_sfid                                  UNIQUE (sfid);
ALTER TABLE powersales.inspection_theme                                ADD CONSTRAINT uk_inspection_theme_sfid                                UNIQUE (sfid);
ALTER TABLE powersales.monthly_female_employee_integration_schedule    ADD CONSTRAINT uk_monthly_female_employee_integration_schedule_sfid    UNIQUE (sfid);
ALTER TABLE powersales.monthly_sales_history                           ADD CONSTRAINT uk_monthly_sales_history_sfid                           UNIQUE (sfid);
ALTER TABLE powersales.notice                                          ADD CONSTRAINT uk_notice_sfid                                          UNIQUE (sfid);
ALTER TABLE powersales.order_request                                   ADD CONSTRAINT uk_order_request_sfid                                   UNIQUE (sfid);
ALTER TABLE powersales.order_request_product                           ADD CONSTRAINT uk_order_request_product_sfid                           UNIQUE (sfid);
ALTER TABLE powersales.promotion_product                               ADD CONSTRAINT uk_promotion_product_sfid                               UNIQUE (sfid);
ALTER TABLE powersales.push_message                                    ADD CONSTRAINT uk_push_message_sfid                                    UNIQUE (sfid);
ALTER TABLE powersales.push_message_receiver                           ADD CONSTRAINT uk_push_message_receiver_sfid                           UNIQUE (sfid);
ALTER TABLE powersales.team_member_schedule                            ADD CONSTRAINT uk_team_member_schedule_sfid                            UNIQUE (sfid);
ALTER TABLE powersales.upload_file                                     ADD CONSTRAINT uk_upload_file_sfid                                     UNIQUE (sfid);
