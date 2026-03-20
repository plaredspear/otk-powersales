-- SafetyCheckSubmission: 복합키 → 서로게이트 PK 전환
-- 기존 (master_id, employee_id, working_date) 복합 PK를 제거하고
-- safety_check_submission_id BIGSERIAL PK + (employee_id, working_date) UNIQUE로 변경

-- 1. 중복 데이터 정리 (employee_id + working_date 기준, 빈 행 제거)
DELETE FROM salesforce2.safety_check_submission a
    USING salesforce2.safety_check_submission b
WHERE a.employee_id = b.employee_id
  AND a.working_date = b.working_date
  AND a.complete_time IS NULL
  AND b.complete_time IS NOT NULL;

-- 2. 기존 복합 PK 제거
ALTER TABLE salesforce2.safety_check_submission
    DROP CONSTRAINT safety_check_submission_pkey;

-- 3. 서로게이트 PK 추가
ALTER TABLE salesforce2.safety_check_submission
    ADD COLUMN safety_check_submission_id BIGSERIAL;

ALTER TABLE salesforce2.safety_check_submission
    ADD CONSTRAINT safety_check_submission_pkey PRIMARY KEY (safety_check_submission_id);

-- 4. master_id 컬럼을 nullable로 변경 (레거시 데이터 보존, 신규 입력 불필요)
ALTER TABLE salesforce2.safety_check_submission
    ALTER COLUMN master_id DROP NOT NULL;

-- 5. 비즈니스 유일성 제약 (하루에 사원 1건)
ALTER TABLE salesforce2.safety_check_submission
    ADD CONSTRAINT uq_safety_check_employee_date UNIQUE (employee_id, working_date);
