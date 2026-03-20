-- 레거시 _mng / 불일치 접미사 테이블명 → 엔티티명 일치 변경 (312)

ALTER TABLE education_code_mng RENAME TO education_code;
ALTER TABLE education_mng RENAME TO education_post;
ALTER TABLE education_file_mng RENAME TO education_post_attachment;
ALTER TABLE education_member_history RENAME TO education_view_history;
ALTER TABLE employee_admin_mng RENAME TO employee_admin;
ALTER TABLE device_version_mng RENAME TO device_version;
