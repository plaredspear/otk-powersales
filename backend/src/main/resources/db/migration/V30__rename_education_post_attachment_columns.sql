-- education_post_attachment 테이블 컬럼명 가독성 개선
-- edu_ 접두사 제거 + 약어 확장 + 부모 PK명 일치 (#392)

ALTER TABLE education_post_attachment RENAME COLUMN edu_id TO education_post_id;
ALTER TABLE education_post_attachment RENAME COLUMN edu_file_key TO file_key;
ALTER TABLE education_post_attachment RENAME COLUMN edu_file_type TO file_type;
ALTER TABLE education_post_attachment RENAME COLUMN edu_file_orgnm TO file_original_name;
