-- Spec #704: AccountCategoryMaster 엔티티 SF Object 정합 (Group A + Reference R-2)
--
-- README §6 SF Object ↔ Entity 정합 정책의 두 번째 적용. #703 Account 패턴 답습.
-- AccountCategoryMaster 의 SF 시스템 자동 메타(Group A) 중 CreatedById / LastModifiedById,
-- 그리고 Reference 필드 OwnerId 에 대해 sfid buffer + Employee FK 컬럼을 추가.
--
-- - *_sfid: Heroku Connect sync / SalesforceMigrationTool 이 채우는 buffer (SF User Id).
--   FK 미부착 (SF 원본 식별자 보존).
-- - *_id: application/SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.
--   ON DELETE SET NULL — 참조 대상 삭제 시 AccountCategoryMaster 행은 보존.
--
-- BaseEntity 의 created_at / updated_at 은 기존 컬럼 (SF CreatedDate / LastModifiedDate
-- 매핑) — 어노테이션만 부여, DB 스키마 변경 없음.

ALTER TABLE powersales.account_category_master
    ADD COLUMN owner_sfid            VARCHAR(18) NULL,
    ADD COLUMN owner_id              BIGINT      NULL,
    ADD COLUMN created_by_sfid       VARCHAR(18) NULL,
    ADD COLUMN created_by_id         BIGINT      NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id   BIGINT      NULL;

ALTER TABLE powersales.account_category_master
    ADD CONSTRAINT fk_account_category_master_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_category_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_category_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함 — backend-conventions §"DB 인덱스 정책")
CREATE INDEX idx_account_category_master_owner_id            ON powersales.account_category_master (owner_id);
CREATE INDEX idx_account_category_master_created_by_id       ON powersales.account_category_master (created_by_id);
CREATE INDEX idx_account_category_master_last_modified_by_id ON powersales.account_category_master (last_modified_by_id);
