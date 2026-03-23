-- ============================================================
-- V38: EducationPostAttachment 자체 PK 전환 + EducationPost FK 연결
-- 데이터 0건이므로 테이블 재구성
-- ============================================================

-- 1. 기존 PK 제약조건 삭제
ALTER TABLE education_post_attachment
    DROP CONSTRAINT IF EXISTS education_post_attachment_pkey;

-- 2. 기존 education_post_id VARCHAR(20) 컬럼 삭제
ALTER TABLE education_post_attachment
    DROP COLUMN IF EXISTS education_post_id;

-- 3. education_post_attachment_id 컬럼 추가 (BIGINT IDENTITY)
ALTER TABLE education_post_attachment
    ADD COLUMN education_post_attachment_id BIGINT GENERATED ALWAYS AS IDENTITY;

-- 4. education_post_attachment_id를 PK로 설정
ALTER TABLE education_post_attachment
    ADD CONSTRAINT education_post_attachment_pkey PRIMARY KEY (education_post_attachment_id);

-- 5. education_post_id 컬럼 추가 (BIGINT NOT NULL, FK용)
ALTER TABLE education_post_attachment
    ADD COLUMN education_post_id BIGINT NOT NULL;

-- 6. FK 제약조건 추가
ALTER TABLE education_post_attachment
    ADD CONSTRAINT fk_attachment_education_post
        FOREIGN KEY (education_post_id) REFERENCES education_post(education_post_id);

-- 7. (education_post_id, file_key) 복합 UNIQUE 제약조건 추가
ALTER TABLE education_post_attachment
    ADD CONSTRAINT uq_attachment_post_file_key UNIQUE (education_post_id, file_key);
