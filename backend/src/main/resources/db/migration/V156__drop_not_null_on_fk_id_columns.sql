-- =========================================================================
-- V156 — SF entity 의 @Column 직접 FK id 컬럼 NOT NULL 일괄 제거
-- =========================================================================
-- 배경:
--   V155 는 @JoinColumn(nullable=false) 만 처리. 그러나 일부 entity 는
--   FK 를 @JoinColumn 관계 대신 @Column 으로 직접 매핑 (예:
--   PromotionEmployee.promotion_id @Column nullable=false + val Long).
--   Stage 1 raw INSERT 는 sfid 만 채우고 FK id 는 Stage 2 resolve 에서
--   채우므로, 동일하게 NOT NULL 위반 발생.
--
-- 결정:
--   V155 와 같은 방침. SF 마이그레이션 대상 entity 의 @Column FK id
--   컬럼을 nullable 로 전환. Entity 타입은 Long? = null 로 보강.
--
-- 대상 (6개 컬럼 / 5개 테이블):
--   agreement_history.employee_id, agreement_word_id
--   alternative_holiday.employee_id
--   professional_promotion_team_history.employee_id
--   professional_promotion_team_master.employee_id, account_id
--   promotion_employee.promotion_id
-- =========================================================================

BEGIN;

-- agreement_history
ALTER TABLE powersales.agreement_history                  ALTER COLUMN employee_id        DROP NOT NULL;
ALTER TABLE powersales.agreement_history                  ALTER COLUMN agreement_word_id  DROP NOT NULL;

-- alternative_holiday
ALTER TABLE powersales.alternative_holiday                ALTER COLUMN employee_id        DROP NOT NULL;

-- professional_promotion_team_history
ALTER TABLE powersales.professional_promotion_team_history ALTER COLUMN employee_id       DROP NOT NULL;

-- professional_promotion_team_master
ALTER TABLE powersales.professional_promotion_team_master  ALTER COLUMN employee_id       DROP NOT NULL;
ALTER TABLE powersales.professional_promotion_team_master  ALTER COLUMN account_id        DROP NOT NULL;

-- promotion_employee
ALTER TABLE powersales.promotion_employee                  ALTER COLUMN promotion_id      DROP NOT NULL;

COMMIT;
