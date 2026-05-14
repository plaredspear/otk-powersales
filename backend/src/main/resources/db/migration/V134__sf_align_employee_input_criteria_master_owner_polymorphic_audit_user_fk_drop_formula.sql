-- EmployeeInputCriteriaMaster (EmployeeInputCriteriaMaster__c) SF Object 정합 (sf-meta-diff EmployeeInputCriteriaMaster__c.md).
--
-- 적용 항목:
-- (Q1) OwnerId polymorphic R-2 — referenceTo=[Group, User]. 단일 Employee FK → User/Group 분기 + XOR CHECK.
-- (Q2) CreatedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q3) LastModifiedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q4) Formula 컬럼 6건 제거 — §6.7 위반.
--      - confirm_alert (ConfirmAlert__c)            : UI 안내 IMAGE
--      - account_categorized_code (AccountCategorizedCode__c) : Category__r.AccountCode__c (Kotlin 재현)
--      - bifurcation_half_person_min_amount_in_realm_ran (BifurcationHalfPersonMinAmountInRealmRan__c) : 산식 (Kotlin 재현)
--      - fixed_1_person_min_amount_in_realm_range (Fixed1PersonMinAmountInRealmRange__c) : 산식 (Kotlin 재현)
--      - valid_data (ValidData__c)                  : TODAY() 의존 산식 (Kotlin 재현)
--      - valid (Valid__c)                           : UI 신호등 IMAGE
--
-- 패턴 출처: V132 (Promotion 동일 변환).

-- (1) 기존 owner / audit FK 제약 + owner_id 컬럼 + idx 제거 (V98 명시 이름)
ALTER TABLE powersales.employee_input_criteria_master
    DROP CONSTRAINT fk_eicm_owner,
    DROP CONSTRAINT fk_eicm_created_by,
    DROP CONSTRAINT fk_eicm_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_eicm_owner_id;

ALTER TABLE powersales.employee_input_criteria_master
    DROP COLUMN owner_id;

-- (2) OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.employee_input_criteria_master
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.employee_input_criteria_master
    ADD CONSTRAINT fk_eicm_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_eicm_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 허용)
ALTER TABLE powersales.employee_input_criteria_master
    ADD CONSTRAINT chk_eicm_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- (3) audit FK 재연결 (employee → user)
ALTER TABLE powersales.employee_input_criteria_master
    ADD CONSTRAINT fk_eicm_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_eicm_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (4) FK 인덱스 (owner_user_id / owner_group_id 신규. created_by_id / last_modified_by_id 는 V98 에서 이미 생성 — 유지)
CREATE INDEX idx_eicm_owner_user_id  ON powersales.employee_input_criteria_master (owner_user_id);
CREATE INDEX idx_eicm_owner_group_id ON powersales.employee_input_criteria_master (owner_group_id);

-- (5) Formula 컬럼 6건 제거
ALTER TABLE powersales.employee_input_criteria_master
    DROP COLUMN confirm_alert,
    DROP COLUMN account_categorized_code,
    DROP COLUMN bifurcation_half_person_min_amount_in_realm_ran,
    DROP COLUMN fixed_1_person_min_amount_in_realm_range,
    DROP COLUMN valid_data,
    DROP COLUMN valid;
