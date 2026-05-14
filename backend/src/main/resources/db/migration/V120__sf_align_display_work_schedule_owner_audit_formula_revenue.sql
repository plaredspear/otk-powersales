-- DisplayWorkScheduleMaster__c (DisplayWorkSchedule) SF prod 메타 정합 (sf-meta-diff 후속)
--
-- SF `DisplayWorkScheduleMaster__c` 메타 정합 5건:
--   Q1: OwnerId polymorphic [Group, User] 패턴 적용 (owner_user_id + owner_group_id + XOR CHECK)
--   Q2: CreatedById FK 참조 대상 전환 (employee → user)
--   Q3: LastModifiedById FK 참조 대상 전환 (employee → user)
--   Q4: ConfirmationAlert__c Formula 컬럼 제거 (§6.7 — Formula 는 DB 컬럼 부재)
--   Q5: LastMonthRevenue__c 타입 정합 BIGINT → NUMERIC(18,0) (SF double precision=18 scale=0)
--
-- 패턴 출처: V90 (BranchReview owner+audit) / V108 (Formula 컬럼 제거) / V115 (Account audit FK 전환).

-- (1) 기존 owner / audit FK 제약 + owner_id 컬럼 + owner_id 인덱스 제거
ALTER TABLE powersales.display_work_schedule
    DROP CONSTRAINT IF EXISTS fk_display_work_schedule_owner,
    DROP CONSTRAINT IF EXISTS fk_display_work_schedule_created_by,
    DROP CONSTRAINT IF EXISTS fk_display_work_schedule_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_display_work_schedule_owner_id;

ALTER TABLE powersales.display_work_schedule
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.display_work_schedule
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.display_work_schedule
    ADD CONSTRAINT fk_display_work_schedule_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_display_work_schedule_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.display_work_schedule
    ADD CONSTRAINT chk_display_work_schedule_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

CREATE INDEX idx_display_work_schedule_owner_user_id  ON powersales.display_work_schedule (owner_user_id);
CREATE INDEX idx_display_work_schedule_owner_group_id ON powersales.display_work_schedule (owner_group_id);

-- (3) audit FK 값 reset 후 user 로 재바인딩
--   Employee.id → User.id 의미 재매핑 필요. 운영 데이터는 마이그레이션 도구로 재적재 예정 (V115 정책 동일).
UPDATE powersales.display_work_schedule
SET created_by_id       = NULL,
    last_modified_by_id = NULL;

ALTER TABLE powersales.display_work_schedule
    ADD CONSTRAINT fk_display_work_schedule_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_display_work_schedule_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- audit FK 인덱스는 V83 에서 이미 생성 — 유지.

-- (4) ConfirmationAlert__c Formula 컬럼 제거 (§6.7)
ALTER TABLE powersales.display_work_schedule
    DROP COLUMN confirmation_alert;

-- (5) LastMonthRevenue__c 타입 정합 (SF double precision=18 scale=0 → NUMERIC(18,0))
ALTER TABLE powersales.display_work_schedule
    ALTER COLUMN last_month_revenue TYPE NUMERIC(18, 0)
    USING last_month_revenue::numeric(18, 0);
