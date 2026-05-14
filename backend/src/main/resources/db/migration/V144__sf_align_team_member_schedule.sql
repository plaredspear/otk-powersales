-- 스펙 #762: TeamMemberSchedule SF Object 정합
--
-- (1) OwnerId polymorphic R-2 (`referenceTo = [Group, User]`).
--     기존 owner_id (Employee FK) 를 owner_user_id (User FK) 로 rename + owner_group_id (Group FK) 신규.
--     CHECK XOR `chk_team_member_schedule_owner_xor` 으로 둘 다 채움 금지.
-- (2) audit FK Employee → User 전환 (CreatedById / LastModifiedById 의 `referenceTo = [User]` 정합).
-- (3) 타입·길이 정합 3건
--       traversal_flag : 255 → 10  (SF length=10)
--       hr_code        : 40  → 255 (SF length=255, 절단 회피)
--       promotion_emp_id_ext : 40 → 30 (SF length=30)
-- (4) WorkingCategory2 picklist enum 정합 — TEMPORARY("임시") 제거.
--     운영 데이터 `working_category2 = '임시'` 행은 NULL 로 변환.
-- (5) Formula 컬럼 7건 DROP — §6.7 entity 컬럼 추가 금지 정책 정합.
--       actual_work_date / commute_date / confirm_alt_holiday_date / dk_day /
--       reason / second_work_type_text / is_work_report
--
-- 데이터 처리:
--   - owner_id / created_by_id / last_modified_by_id 의 Employee.id 잔여값은 User.user_id 와 매칭 안 되므로 NULL 초기화.
--   - owner_sfid / created_by_sfid / last_modified_by_sfid sync buffer 보존 → sf-migrate Phase 2 가
--     `<관계>_sfid` → `user.sfid` / `group.sfid` lookup 으로 FK 자동 채움.
--
-- 관련: V89 (audit/owner Employee FK 도입), V102 (Formula 컬럼 6건 추가),
--       V143 (MFEIS owner polymorphic + audit User FK 선례).

-- ============================================================================
-- (1) OwnerId polymorphic R-2 — owner_id → owner_user_id rename + owner_group_id 신규
-- ============================================================================

-- 기존 Employee FK 제약 + 인덱스 제거
ALTER TABLE powersales.team_member_schedule
    DROP CONSTRAINT fk_team_member_schedule_owner;

DROP INDEX powersales.idx_team_member_schedule_owner_id;

-- 컬럼 rename: owner_id → owner_user_id
ALTER TABLE powersales.team_member_schedule
    RENAME COLUMN owner_id TO owner_user_id;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.team_member_schedule SET owner_user_id = NULL;

-- Group FK 컬럼 신규 추가
ALTER TABLE powersales.team_member_schedule
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT fk_team_member_schedule_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_team_member_schedule_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT chk_team_member_schedule_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_team_member_schedule_owner_user_id
    ON powersales.team_member_schedule (owner_user_id);
CREATE INDEX idx_team_member_schedule_owner_group_id
    ON powersales.team_member_schedule (owner_group_id);

-- ============================================================================
-- (2) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.team_member_schedule
    DROP CONSTRAINT fk_team_member_schedule_created_by,
    DROP CONSTRAINT fk_team_member_schedule_last_modified_by;

-- Employee.id 잔여값 NULL 초기화 (Phase 2 재채움)
UPDATE powersales.team_member_schedule
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT fk_team_member_schedule_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_team_member_schedule_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V89 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (3) 타입·길이 정합 3건
-- ============================================================================

ALTER TABLE powersales.team_member_schedule
    ALTER COLUMN traversal_flag TYPE VARCHAR(10),
    ALTER COLUMN hr_code TYPE VARCHAR(255),
    ALTER COLUMN promotion_emp_id_ext TYPE VARCHAR(30);

-- ============================================================================
-- (4) WorkingCategory2 picklist 정합 — `임시` 값 NULL 변환
-- ============================================================================

UPDATE powersales.team_member_schedule
    SET working_category2 = NULL
    WHERE working_category2 = '임시';

-- ============================================================================
-- (5) Formula 컬럼 7건 DROP — §6.7 정책 위반 해소
-- ============================================================================

ALTER TABLE powersales.team_member_schedule
    DROP COLUMN actual_work_date,
    DROP COLUMN commute_date,
    DROP COLUMN confirm_alt_holiday_date,
    DROP COLUMN dk_day,
    DROP COLUMN reason,
    DROP COLUMN second_work_type_text,
    DROP COLUMN is_work_report;
