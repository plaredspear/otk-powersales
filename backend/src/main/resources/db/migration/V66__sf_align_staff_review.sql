-- Spec #711 — StaffReview SF Object 정합 (Group A + Reference R-2).
--
-- 변경 내용:
--   1. Group A CreatedById / LastModifiedById sfid 버퍼 컬럼 추가
--   2. CreatedById / LastModifiedById → Employee FK 컬럼 + 제약 + 인덱스 추가

ALTER TABLE powersales.staff_review
    ADD COLUMN created_by_sfid      varchar(18),
    ADD COLUMN last_modified_by_sfid varchar(18),
    ADD COLUMN created_by_id        bigint,
    ADD COLUMN last_modified_by_id  bigint;

ALTER TABLE powersales.staff_review
    ADD CONSTRAINT fk_staff_review_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_staff_review_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id);

CREATE INDEX idx_staff_review_created_by_id
    ON powersales.staff_review (created_by_id);

CREATE INDEX idx_staff_review_last_modified_by_id
    ON powersales.staff_review (last_modified_by_id);
