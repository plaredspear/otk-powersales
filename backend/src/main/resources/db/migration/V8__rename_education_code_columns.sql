-- Spec #353: EducationCode 컬럼명 가독성 개선
ALTER TABLE education_code RENAME COLUMN edu_code_nm TO edu_code_name;
