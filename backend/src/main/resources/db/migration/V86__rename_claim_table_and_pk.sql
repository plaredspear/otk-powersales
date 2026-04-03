-- Claim 테이블/컬럼명 가독성 개선 (#512)
-- 1. PK 컬럼명 변경 (FK 참조보다 선행)
ALTER TABLE claims RENAME COLUMN id TO claim_id;

-- 2. 테이블명 단수형 변경
ALTER TABLE claims RENAME TO claim;
