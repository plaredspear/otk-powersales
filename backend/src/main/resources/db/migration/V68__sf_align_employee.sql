-- Spec #713: Employee 엔티티 SF Object 정합 (Group A + Reference R-2)
--
-- README §6 SF Object ↔ Entity 정합 정책 Employee 적용.
--
-- - office_phone: OfficePhone__c 신규 도입 (Q2: D 분류 누락 필드).
-- - crm_work_type: DKRetail__CRM_WorkType__c picklist 신규 도입.
-- - *_sfid: sync buffer 컬럼 (SF User/Employee Id). FK 미부착 (SF 원본 식별자 보존).
-- - *_id: SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.
--   ON DELETE SET NULL — 참조 대상 삭제 시 Employee 행 보존.
-- - manager_id: DKRetail__ManagerId__c 셀프 reference. 상급자 계층 보존.
--
-- IsDeleted @SFField 추가 / DKRetail__Sex__c Converter 전환 / HCColumn 추가 — DB 스키마 변경 없음.
-- BaseEntity 의 created_at / updated_at 은 기존 컬럼 — 어노테이션만 부여, 스키마 변경 없음.

ALTER TABLE powersales.employee
    ADD COLUMN office_phone          VARCHAR(40)  NULL,
    ADD COLUMN crm_work_type         VARCHAR(255) NULL,
    ADD COLUMN owner_sfid            VARCHAR(18)  NULL,
    ADD COLUMN created_by_sfid       VARCHAR(18)  NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18)  NULL,
    ADD COLUMN owner_id              BIGINT       NULL,
    ADD COLUMN created_by_id         BIGINT       NULL,
    ADD COLUMN last_modified_by_id   BIGINT       NULL,
    ADD COLUMN manager_id            BIGINT       NULL;

ALTER TABLE powersales.employee
    ADD CONSTRAINT fk_employee_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_employee_manager
        FOREIGN KEY (manager_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함 — backend-conventions §"DB 인덱스 정책")
CREATE INDEX idx_employee_owner_id              ON powersales.employee (owner_id);
CREATE INDEX idx_employee_created_by_id         ON powersales.employee (created_by_id);
CREATE INDEX idx_employee_last_modified_by_id   ON powersales.employee (last_modified_by_id);
CREATE INDEX idx_employee_manager_id            ON powersales.employee (manager_id);
