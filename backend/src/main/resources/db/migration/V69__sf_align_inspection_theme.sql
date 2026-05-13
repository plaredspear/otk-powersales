-- Spec #714: InspectionTheme 엔티티 SF Object 정합 (Group A + Reference R-2)
--
-- README §6 SF Object ↔ Entity 정합 정책 InspectionTheme 적용.
--
-- - *_sfid: sync buffer 컬럼 (SF User Id). FK 미부착 (SF 원본 식별자 보존).
-- - *_id: SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.
--   ON DELETE SET NULL — 참조 대상 삭제 시 InspectionTheme 행 보존.
--
-- IsDeleted @SFField 추가 / BaseEntity 전환 (updated_at HC 매핑 변경) — DB 스키마 변경 없음.
-- BaseEntity 의 created_at / updated_at 은 기존 컬럼 — 어노테이션만 부여, 스키마 변경 없음.

ALTER TABLE powersales.inspection_theme
    ADD COLUMN owner_sfid            VARCHAR(18) NULL,
    ADD COLUMN created_by_sfid       VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id              BIGINT      NULL,
    ADD COLUMN created_by_id         BIGINT      NULL,
    ADD COLUMN last_modified_by_id   BIGINT      NULL;

ALTER TABLE powersales.inspection_theme
    ADD CONSTRAINT fk_inspection_theme_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_inspection_theme_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_inspection_theme_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함 — backend-conventions §"DB 인덱스 정책")
CREATE INDEX idx_inspection_theme_owner_id              ON powersales.inspection_theme (owner_id);
CREATE INDEX idx_inspection_theme_created_by_id         ON powersales.inspection_theme (created_by_id);
CREATE INDEX idx_inspection_theme_last_modified_by_id   ON powersales.inspection_theme (last_modified_by_id);
