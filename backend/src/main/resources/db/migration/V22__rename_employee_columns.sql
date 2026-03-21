-- employee 테이블 PK 및 레거시 컬럼명 가독성 개선 (#381)
ALTER TABLE employee RENAME COLUMN id TO employee_id;
ALTER TABLE employee RENAME COLUMN isdeleted TO is_deleted;
