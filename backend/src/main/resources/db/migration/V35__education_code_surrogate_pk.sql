-- EducationCode 자체 PK 전환: 비즈니스 키(VARCHAR) → surrogate PK(BIGINT IDENTITY) + edu_code 컬럼 추가
-- Spec #397

-- 1. 기존 PK 제약조건 삭제
ALTER TABLE salesforce2.education_code DROP CONSTRAINT IF EXISTS education_code_pkey;

-- 2. 기존 education_code_id VARCHAR 컬럼 삭제
ALTER TABLE salesforce2.education_code DROP COLUMN education_code_id;

-- 3. education_code_id 컬럼 추가 (BIGINT GENERATED ALWAYS AS IDENTITY)
ALTER TABLE salesforce2.education_code ADD COLUMN education_code_id BIGINT GENERATED ALWAYS AS IDENTITY;

-- 4. education_code_id 컬럼을 PK로 설정
ALTER TABLE salesforce2.education_code ADD CONSTRAINT education_code_pkey PRIMARY KEY (education_code_id);

-- 5. edu_code 컬럼 추가 (VARCHAR(20) NOT NULL)
ALTER TABLE salesforce2.education_code ADD COLUMN edu_code VARCHAR(20) NOT NULL;

-- 6. edu_code 컬럼에 UNIQUE 제약조건 추가
ALTER TABLE salesforce2.education_code ADD CONSTRAINT uq_education_code_edu_code UNIQUE (edu_code);
