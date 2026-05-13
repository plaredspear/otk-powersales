-- 스펙 #716: HolidayMaster SF Object 정합 (Group A + Reference R-2)
--
-- 변경 사항:
-- - name 길이 50 → 80 (SF describe `Name` length 80 정합, 절단 방지 정책)
-- - type 길이 20 → 255 (SF describe `Type__c` picklist length 255 정합)
-- - owner_sfid / owner_id 신규 (Owner R-2 패턴)
-- - created_by_sfid / created_by_id 신규 (CreatedById R-2 패턴)
-- - last_modified_by_sfid / last_modified_by_id 신규 (LastModifiedById R-2 패턴)
-- - FK 제약: 3개 모두 employee.employee_id 참조 (ON DELETE SET NULL)

ALTER TABLE powersales.holiday_master
    ALTER COLUMN name TYPE VARCHAR(80);

ALTER TABLE powersales.holiday_master
    ALTER COLUMN type TYPE VARCHAR(255);

ALTER TABLE powersales.holiday_master
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.holiday_master
    ADD CONSTRAINT fk_holiday_master_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_holiday_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_holiday_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_holiday_master_owner_id ON powersales.holiday_master (owner_id);
CREATE INDEX idx_holiday_master_created_by_id ON powersales.holiday_master (created_by_id);
CREATE INDEX idx_holiday_master_last_modified_by_id ON powersales.holiday_master (last_modified_by_id);
