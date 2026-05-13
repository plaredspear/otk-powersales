-- 스펙 #735: StaffReview ↔ BranchReview FK 후처리
--
-- #711 에서 staff_review.branch_review_sfid 컬럼만 추가 (FK 보류).
-- 본 마이그레이션에서 branch_review_id BIGINT FK 컬럼 + 제약 + 인덱스 추가.
-- cascade: BranchReview 삭제 시 StaffReview 보존 (FK 만 NULL 처리).

ALTER TABLE powersales.staff_review
    ADD COLUMN branch_review_id BIGINT;

ALTER TABLE powersales.staff_review
    ADD CONSTRAINT fk_staff_review_branch_review
        FOREIGN KEY (branch_review_id) REFERENCES powersales.branch_review (branch_review_id)
        ON DELETE SET NULL;

CREATE INDEX idx_staff_review_branch_review_id ON powersales.staff_review (branch_review_id);
