-- employee_info 테이블 레거시 컬럼명 → 가독성 있는 이름으로 변경
ALTER TABLE employee_info RENAME COLUMN emp_pwd TO password;
ALTER TABLE employee_info RENAME COLUMN pwd_yn TO password_change_required;
ALTER TABLE employee_info RENAME COLUMN emp_uuid TO device_uuid;
ALTER TABLE employee_info RENAME COLUMN emp_token TO fcm_token;
ALTER TABLE employee_info RENAME COLUMN gps_yn TO gps_consent;
ALTER TABLE employee_info RENAME COLUMN gps_yn_date TO gps_consent_date;
