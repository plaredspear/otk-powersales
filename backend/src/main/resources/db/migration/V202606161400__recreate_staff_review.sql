-- StaffReview(사원평가) 도메인 복원 — SF StaffReview__c 데이터 마이그레이션 대상 재도입.
--
-- 배경: V164__drop_review_tables.sql (2026-05-19) 에서 "미사용 도메인" 으로 staff_review /
-- branch_review / hq_review 3개 테이블이 의도적으로 DROP 되었다. 본 마이그레이션은 SF
-- StaffReview__c 데이터 적재 대상으로 staff_review 테이블만 재생성한다 (BranchReview / HqReview
-- 미복원 — 사용자 결정).
--
-- 컬럼 집합 = V164 직전 staff_review 최종 스키마 (V1 base + V44 + V66 + V102 + V149 rename) 중
-- BranchReview FK (V91 branch_review_id) 제외:
--   - branch_review_sfid 는 SF BranchReviews__c buffer 컬럼으로만 보존 (FK 미연결 — branch_review 테이블 부재).
--
-- SF 정합 (prod describe 2026-05-29):
--   - StaffReview__c 에는 OwnerId 필드가 없다 → owner_* 컬럼 미생성.
--   - CreatedById / LastModifiedById referenceTo = [User] → audit FK 는 user(user_id) 참조
--     (현재 컨벤션: SiteActivity / V120 BranchReview audit 와 동일하게 User FK).
--   - DKRetail_EmployeeId__c referenceTo = [DKRetail__Employee__c] → employee_id 는 employee(employee_id) 참조.
--   - Formula 8개 (Branch/CostCenterCode/EmployeeName/EmployeeNumber/EmployeeTotalScore/EmployeeType/
--     EntryDate/Jikwee) 중 일부는 레거시에서 비수식 캐시 컬럼으로 운영되어 DB 컬럼 유지 (V1/V102 기존 스키마 정합).

CREATE TABLE powersales.staff_review (
    staff_review_id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                            VARCHAR(18) UNIQUE,
    name                            VARCHAR(80),
    -- 사원 식별 / 캐시
    employee_id                     BIGINT,
    employee_sfid                   VARCHAR(18),
    employee_name                   VARCHAR(1300),
    employee_code                   VARCHAR(1300),
    branch                          VARCHAR(1300),
    branch_review_sfid              VARCHAR(18),
    cost_center_code                VARCHAR(1300),
    employee_total_score            DOUBLE PRECISION,
    -- 점수 항목
    attendance_score                DOUBLE PRECISION,
    instruction_disobedience_score  DOUBLE PRECISION,
    priority_item_event_score       DOUBLE PRECISION,
    display_event_goal_score        DOUBLE PRECISION,
    account_partnership_score       DOUBLE PRECISION,
    clothes_hygiene_score           DOUBLE PRECISION,
    product_manage_callment_score   DOUBLE PRECISION,
    education_evaluation_score      DOUBLE PRECISION,
    -- 근무유형 / 직무 / 시점 (V44)
    working_category1               VARCHAR(255),
    working_category2               VARCHAR(255),
    working_category3               VARCHAR(255),
    job_code                        VARCHAR(20),
    first_day_of_month              DATE,
    -- 구분 / 입사일 / 직위 (V102)
    employee_type                   VARCHAR(1300),
    entry_date                      DATE,
    jikwee                          VARCHAR(1300),
    -- Group A
    is_deleted                      BOOLEAN,
    created_by_sfid                 VARCHAR(18),
    created_by_id                   BIGINT,
    last_modified_by_sfid           VARCHAR(18),
    last_modified_by_id             BIGINT,
    -- BaseEntity
    created_at                      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_staff_review_employee
        FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_staff_review_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_staff_review_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_staff_review_employee_id ON powersales.staff_review (employee_id);
CREATE INDEX idx_staff_review_created_by_id ON powersales.staff_review (created_by_id);
CREATE INDEX idx_staff_review_last_modified_by_id ON powersales.staff_review (last_modified_by_id);
CREATE INDEX idx_staff_review_first_day_of_month ON powersales.staff_review (first_day_of_month);
