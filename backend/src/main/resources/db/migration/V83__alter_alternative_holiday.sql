-- #491: AlternativeHoliday PK rename + sfid column + employee FK

-- 1. PK 컬럼명 변경: id → alternative_holiday_id
ALTER TABLE alternative_holiday RENAME COLUMN id TO alternative_holiday_id;

-- 2. employee_sfid 컬럼 추가
ALTER TABLE alternative_holiday ADD COLUMN employee_sfid VARCHAR(18);

-- 3. employee_id FK 제약 조건 추가
ALTER TABLE alternative_holiday
    ADD CONSTRAINT fk_alternative_holiday_employee
    FOREIGN KEY (employee_id) REFERENCES employee(employee_id);
