-- PromotionEmployee SF 정합 (sf-meta-diff Q1~Q7):
--
-- (1) Q1~Q3: 수량 컬럼 INTEGER → BIGINT (SF double precision=18 scale=0 정수 도메인 정합).
--     daily_target_count / primary_sales_quantity / other_sales_quantity 컬럼 타입 변경.
-- (2) Q4: dk_work_type2 (SF DKRetail__WorkType2__c picklist) Converter 미적용 → entity 측 WorkingCategory2Converter 적용.
--     DB 컬럼 타입/길이 변경 없음 (varchar 255 유지). entity 어노테이션만 수정 (별도 task).
-- (3) Q5~Q6: audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 referenceTo=[User] 정합).
-- (4) Q7: work_type2 컬럼 + @SFField("WorkType2__c") 매핑 제거.
--     SF WorkType2__c 는 calculated=true Formula (DKRetail__PromotionId__r.Category1__c → Promotion.Category1__c → DKRetail__PrimaryProductId__r.StoreCondition__c).
--     사용자 결정 (옵션 b): PromotionEmployee.workType2 완전 제거 + 대체 접근은 FK chain (promotion.primaryProduct.storeConditionText) 사용.
--     §6.7 정책 정합 — Formula 필드는 backend 컬럼으로 두지 않음.
--
-- 데이터 처리:
--   - created_by_id / last_modified_by_id 기존 값은 Employee.employee_id 였으므로 User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - 운영에서는 created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     SalesforceMigrationTool Phase 2 lookup (`<관계>_sfid` → `user.sfid` → `user.user_id`) 으로 FK 자동 채움.
--   - work_type2 컬럼은 사용처 0건 (DTO/Service 참조 없음) → drop 시 데이터 손실 영향 없음.
--   - owner 컬럼군은 SF prod 메타에 OwnerId 부재 (sobject ownable=false, master-detail 자식) → 운영 메타 부재 (보존) 처리. 본 마이그레이션 변경 대상 아님.
--
-- 관련: V85__sf_align_promotion_employee.sql (audit/owner Employee FK 도입), V96__drop_sf_formula_columns_740.sql (work_type4 등 Formula 컬럼 drop 선례), V128__sf_align_order_request_owner_polymorphic_audit_user_fk_precision.sql (audit FK User 패턴 선례).

-- ============================================================================
-- (1) 수량 컬럼 INTEGER → BIGINT (Q1~Q3)
-- ============================================================================

ALTER TABLE powersales.promotion_employee
    ALTER COLUMN daily_target_count    TYPE BIGINT,
    ALTER COLUMN primary_sales_quantity TYPE BIGINT,
    ALTER COLUMN other_sales_quantity   TYPE BIGINT;

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User (Q5~Q6)
-- ============================================================================

ALTER TABLE powersales.promotion_employee
    DROP CONSTRAINT fk_promotion_employee_created_by,
    DROP CONSTRAINT fk_promotion_employee_last_modified_by;

UPDATE powersales.promotion_employee
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.promotion_employee
    ADD CONSTRAINT fk_promotion_employee_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_employee_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (3) work_type2 컬럼 drop — SF Formula 정합 (Q7)
-- ============================================================================

ALTER TABLE powersales.promotion_employee DROP COLUMN IF EXISTS work_type2;
