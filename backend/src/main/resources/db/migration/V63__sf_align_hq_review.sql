-- Spec #708: HqReview SF Object 정합 (Group A + Reference R-2)
--
-- Group A Reference (OwnerId / CreatedById / LastModifiedById) sfid buffer + Employee FK 추가.
-- IsDeleted: 기존 is_deleted 컬럼에 @SFField 어노테이션만 추가 — DB 스키마 변경 없음.
-- BaseEntity created_at / updated_at: 기존 컬럼 — 어노테이션만 부여, DB 스키마 변경 없음.
-- EvaluationyType__c: varchar 컬럼 유지, EvaluationType enum + Converter 로 애플리케이션 레이어 변환.

ALTER TABLE powersales.hq_review
    ADD COLUMN owner_sfid            VARCHAR(18),
    ADD COLUMN owner_id              BIGINT,
    ADD COLUMN created_by_sfid       VARCHAR(18),
    ADD COLUMN created_by_id         BIGINT,
    ADD COLUMN last_modified_by_sfid VARCHAR(18),
    ADD COLUMN last_modified_by_id   BIGINT;

ALTER TABLE powersales.hq_review
    ADD CONSTRAINT fk_hq_review_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_hq_review_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_hq_review_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함 — backend-conventions §"DB 인덱스 정책")
CREATE INDEX idx_hq_review_owner_id             ON powersales.hq_review (owner_id);
CREATE INDEX idx_hq_review_created_by_id        ON powersales.hq_review (created_by_id);
CREATE INDEX idx_hq_review_last_modified_by_id  ON powersales.hq_review (last_modified_by_id);
