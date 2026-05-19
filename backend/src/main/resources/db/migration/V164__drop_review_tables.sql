-- Drop review tables (branch_review, hq_review, staff_review) and related references.
-- monthly_sales_history.hq_review_id / hq_review_sfid 컬럼 제거 + 3개 테이블 drop.
-- FK 의존 순서: monthly_sales_history 컬럼 → staff_review (branch_review FK) → branch_review → hq_review.

ALTER TABLE powersales.monthly_sales_history DROP COLUMN IF EXISTS hq_review_id;
ALTER TABLE powersales.monthly_sales_history DROP COLUMN IF EXISTS hq_review_sfid;

DROP TABLE IF EXISTS powersales.staff_review CASCADE;
DROP TABLE IF EXISTS powersales.branch_review CASCADE;
DROP TABLE IF EXISTS powersales.hq_review CASCADE;
