-- Spec #307: Group E 엔티티 BaseEntity 적용 - 타임스탬프 컬럼 추가
-- 대상: product_barcode, employee_admin_mng, education_code_mng, safety_check_submission, safetycheck_list

-- product_barcode
ALTER TABLE product_barcode ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE product_barcode ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- employee_admin_mng
ALTER TABLE employee_admin_mng ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE employee_admin_mng ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- education_code_mng
ALTER TABLE education_code_mng ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE education_code_mng ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- safety_check_submission
ALTER TABLE safety_check_submission ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE safety_check_submission ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- safetycheck_list
ALTER TABLE safetycheck_list ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE safetycheck_list ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
