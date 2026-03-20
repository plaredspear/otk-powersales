-- 1. 테이블 리네임
ALTER TABLE safetycheck_list RENAME TO safety_check_item;

-- 2. 기존 복합 PK 제약조건 제거
ALTER TABLE safety_check_item DROP CONSTRAINT safetycheck_list_pk;

-- 3. 대리 키 컬럼 추가 + PK 설정
ALTER TABLE safety_check_item ADD COLUMN safety_check_item_id BIGSERIAL;
ALTER TABLE safety_check_item ADD CONSTRAINT safety_check_item_pkey PRIMARY KEY (safety_check_item_id);

-- 4. 기존 복합 키에 unique 제약조건 추가
ALTER TABLE safety_check_item ADD CONSTRAINT uq_safety_check_item_question_seq UNIQUE (question_num, seq_num);
