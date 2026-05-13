-- 스펙 #732: DisplayWorkSchedule SF Object 정합 (Group A + Reference R-2 + 길이 정합)

-- SF 정합 길이 확장 (절단 방지)
-- type_of_work4: 20 → 255 (SF picklist length 255)
ALTER TABLE powersales.display_work_schedule
    ALTER COLUMN type_of_work4 TYPE VARCHAR(255);

-- Group A R-2: CreatedBy / LastModifiedBy (owner_id / owner_sfid 는 V1 기존)
ALTER TABLE powersales.display_work_schedule
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.display_work_schedule
    ADD CONSTRAINT fk_display_work_schedule_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_display_work_schedule_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_display_work_schedule_created_by_id ON powersales.display_work_schedule (created_by_id);
CREATE INDEX idx_display_work_schedule_last_modified_by_id ON powersales.display_work_schedule (last_modified_by_id);

-- owner_id FK 제약은 V1 에 없는 경우 추가 (idempotent 처리)
ALTER TABLE powersales.display_work_schedule
    ADD CONSTRAINT fk_display_work_schedule_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_display_work_schedule_owner_id ON powersales.display_work_schedule (owner_id);
