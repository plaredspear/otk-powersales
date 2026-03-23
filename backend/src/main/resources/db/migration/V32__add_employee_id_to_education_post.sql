-- EducationPost에 employee_id FK 컬럼 추가 (#394)
ALTER TABLE education_post ADD COLUMN employee_id BIGINT;

ALTER TABLE education_post
    ADD CONSTRAINT fk_education_post_employee
    FOREIGN KEY (employee_id) REFERENCES employee(employee_id);
