ALTER TABLE powersales.promotion_employee
    ADD COLUMN IF NOT EXISTS description  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS work_type2   VARCHAR(255);
