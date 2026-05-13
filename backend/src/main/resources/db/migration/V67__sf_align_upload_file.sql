-- Spec #712 — UploadFile SF Object 정합 (Group A + Reference R-2).
--
-- 변경 내용:
--   1. Group A OwnerId / CreatedById / LastModifiedById sfid 버퍼 컬럼 추가
--   2. OwnerId / CreatedById / LastModifiedById → Employee FK 컬럼 + 제약 + 인덱스 추가

ALTER TABLE powersales.upload_file
    ADD COLUMN owner_sfid            varchar(18),
    ADD COLUMN created_by_sfid       varchar(18),
    ADD COLUMN last_modified_by_sfid varchar(18),
    ADD COLUMN owner_id              bigint,
    ADD COLUMN created_by_id         bigint,
    ADD COLUMN last_modified_by_id   bigint;

ALTER TABLE powersales.upload_file
    ADD CONSTRAINT fk_upload_file_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_upload_file_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_upload_file_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id);

CREATE INDEX idx_upload_file_owner_id
    ON powersales.upload_file (owner_id);

CREATE INDEX idx_upload_file_created_by_id
    ON powersales.upload_file (created_by_id);

CREATE INDEX idx_upload_file_last_modified_by_id
    ON powersales.upload_file (last_modified_by_id);
