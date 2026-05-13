-- 스펙 #736: Appointment SF Object 정합 (Group A R-2 + 길이 정합 + sfid)

-- sfid 컬럼 신규 추가 (UNIQUE)
ALTER TABLE powersales.appointment
    ADD COLUMN sfid VARCHAR(18);

CREATE UNIQUE INDEX idx_appointment_sfid_unique ON powersales.appointment (sfid);

-- SF 정합 길이 확장 (절단 방지 — 12개)
ALTER TABLE powersales.appointment
    ALTER COLUMN employee_code   TYPE VARCHAR(100),
    ALTER COLUMN jikchak         TYPE VARCHAR(100),
    ALTER COLUMN jikgub          TYPE VARCHAR(100),
    ALTER COLUMN jikjong         TYPE VARCHAR(100),
    ALTER COLUMN jikwee          TYPE VARCHAR(100),
    ALTER COLUMN job_code        TYPE VARCHAR(100),
    ALTER COLUMN manage_type     TYPE VARCHAR(100),
    ALTER COLUMN ord_detail_code TYPE VARCHAR(100),
    ALTER COLUMN ord_detail_node TYPE VARCHAR(250),
    ALTER COLUMN after_org_code  TYPE VARCHAR(100),
    ALTER COLUMN work_area       TYPE VARCHAR(100),
    ALTER COLUMN work_type       TYPE VARCHAR(100);

-- Group A R-2: IsDeleted + CreatedBy / LastModifiedBy
ALTER TABLE powersales.appointment
    ADD COLUMN is_deleted            BOOLEAN,
    ADD COLUMN created_by_sfid       VARCHAR(18),
    ADD COLUMN created_by_id         BIGINT,
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT;

ALTER TABLE powersales.appointment
    ADD CONSTRAINT fk_appointment_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_appointment_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_appointment_created_by_id ON powersales.appointment (created_by_id);
CREATE INDEX idx_appointment_last_modified_by_id ON powersales.appointment (last_modified_by_id);
