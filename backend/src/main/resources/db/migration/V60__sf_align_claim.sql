-- Spec #705: Claim 엔티티 SF Object 정합 (Group A + Reference R-2 + Status enum SF 정합)
--
-- README §6 SF Object ↔ Entity 정합 정책 — Claim 적용.
--
-- 변경 사항:
--   1. BaseEntity 상속 (Q1 옵션 1) — updated_at 컬럼 신규. 기존 created_at 은 유지.
--   2. IsDeleted (Group A) — is_deleted 컬럼 신규.
--   3. Reference R-2 — OwnerId / CreatedById / LastModifiedById sfid buffer + Employee FK.
--   4. Reference R-2 — Product FK 신규 (기존 product_sfid 만 보유, FK 없음).
--   5. Account FK 컬럼 리네임 — store_id → account_id (R-2 패턴 컬럼명 표준).
--   6. Status enum SF 정합 (Q4) — dev 데이터 폐기 + Converter 신규로 SF 한국어 옵션값 보존.
--      기존 ClaimStatus (SUBMITTED/IN_PROGRESS/RESOLVED/REJECTED) → SF 정합 (DRAFT/SENT/SEND_FAILED).
--
-- *_sfid: Heroku Connect sync / SalesforceMigrationTool 이 채우는 buffer (SF User Id / SF Product Id).
-- *_id: SF reference → Employee/Product 매핑 결과 FK. ON DELETE SET NULL — 참조 대상 삭제 시 Claim 보존.

-- 1) 기존 데이터 정리 (Status enum 옵션값 변경으로 기존 데이터 매칭 불가 — 사용자 결정)
-- claim_photo 가 claim_id FK 보유 — claim 삭제 전 선행 정리 필요.
DELETE FROM powersales.claim_photos;
DELETE FROM powersales.claim;

-- 2) Account FK 컬럼 리네임 (store_id → account_id, idx_claim_store → idx_claim_account)
ALTER TABLE powersales.claim
    RENAME COLUMN store_id TO account_id;
ALTER INDEX powersales.idx_claim_store RENAME TO idx_claim_account;
ALTER TABLE powersales.claim
    RENAME CONSTRAINT claims_store_id_fkey TO fk_claim_account;

-- 3) BaseEntity / Group A / Reference 컬럼 ADD
ALTER TABLE powersales.claim
    ADD COLUMN updated_at            TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN is_deleted            BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN owner_sfid            VARCHAR(18) NULL,
    ADD COLUMN owner_id              BIGINT      NULL,
    ADD COLUMN created_by_sfid       VARCHAR(18) NULL,
    ADD COLUMN created_by_id         BIGINT      NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id   BIGINT      NULL,
    ADD COLUMN product_id            BIGINT      NULL;

-- 4) Status 컬럼 — Converter 가 application 측 변환 담당. DB 스키마 변경 없음 (length=20 유지).

-- 5) FK 제약 (#705 R-2 패턴)
ALTER TABLE powersales.claim
    ADD CONSTRAINT fk_claim_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_claim_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_claim_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_claim_product
        FOREIGN KEY (product_id) REFERENCES powersales.product (product_id)
        ON DELETE SET NULL;

-- 6) FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함 — backend-conventions §"DB 인덱스 정책")
CREATE INDEX idx_claim_owner_id              ON powersales.claim (owner_id);
CREATE INDEX idx_claim_created_by_id         ON powersales.claim (created_by_id);
CREATE INDEX idx_claim_last_modified_by_id   ON powersales.claim (last_modified_by_id);
CREATE INDEX idx_claim_product_id            ON powersales.claim (product_id);
