-- AlternativeHoliday SF 정합 — OwnerId polymorphic + audit FK Employee → User + ChangeReason length + created_by 명명 정리
--
-- (1) OwnerId polymorphic R-2 — SF `DKRetail__AlternativeHoliday__c.OwnerId.referenceTo = [Group, User]`.
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename 하고 owner_group_id (Group FK) 신규 추가.
--     CHECK XOR `chk_alternative_holiday_owner_xor` 로 둘 다 채움 금지.
-- (2) audit FK 재정의 — created_by_id / last_modified_by_id 의 FK 대상 Employee → User.
-- (3) ChangeReason length 정합 — SF textarea(255) ↔ entity VARCHAR(500) → VARCHAR(255).
-- (4) 비-SF application 컬럼 `created_by` (사원번호) → `created_by_emp_no` rename — entity `createdBy: User?` FK 와 명명 충돌 회피.
--
-- 데이터 처리:
--   - 기존 owner_id / created_by_id / last_modified_by_id 값은 Employee.employee_id 였으므로
--     User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - 운영에서는 owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 가 보존되어 있어
--     SalesforceMigrationTool Phase 2 lookup (`<관계>_sfid` → `user.sfid` → `user.user_id`) 으로 FK 자동 채움.
--
-- 관련: V75__sf_align_alternative_holiday.sql (audit/owner Employee FK 도입), V116__sf_align_account_category_master_owner_polymorphic_and_user_fk.sql (선례).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id 를 owner_user_id 로 rename + owner_group_id 신규
-- ============================================================================

ALTER TABLE powersales.alternative_holiday
    DROP CONSTRAINT fk_alternative_holiday_owner;

DROP INDEX powersales.idx_alternative_holiday_owner_id;

ALTER TABLE powersales.alternative_holiday
    RENAME COLUMN owner_id TO owner_user_id;

UPDATE powersales.alternative_holiday SET owner_user_id = NULL;

ALTER TABLE powersales.alternative_holiday
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.alternative_holiday
    ADD CONSTRAINT fk_alternative_holiday_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_alternative_holiday_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

ALTER TABLE powersales.alternative_holiday
    ADD CONSTRAINT chk_alternative_holiday_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_alternative_holiday_owner_user_id  ON powersales.alternative_holiday (owner_user_id);
CREATE INDEX idx_alternative_holiday_owner_group_id ON powersales.alternative_holiday (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.alternative_holiday
    DROP CONSTRAINT fk_alternative_holiday_created_by,
    DROP CONSTRAINT fk_alternative_holiday_last_modified_by;

UPDATE powersales.alternative_holiday
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.alternative_holiday
    ADD CONSTRAINT fk_alternative_holiday_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_alternative_holiday_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- ============================================================================
-- (3) ChangeReason length 좁힘 — SF textarea(255) 정합
-- ============================================================================

ALTER TABLE powersales.alternative_holiday
    ALTER COLUMN change_reason TYPE VARCHAR(255);

-- ============================================================================
-- (4) created_by → created_by_emp_no rename — entity `createdBy: User?` FK 와 명명 충돌 회피
-- ============================================================================

ALTER TABLE powersales.alternative_holiday
    RENAME COLUMN created_by TO created_by_emp_no;
