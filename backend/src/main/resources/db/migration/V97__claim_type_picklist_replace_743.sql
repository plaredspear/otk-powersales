-- Spec #743: Claim Category/Subcategory FK 제거 + ClaimType1/2 picklist enum 적용
--
-- 정책: #741 옵션 C (FK 제거 + enum 대체).
-- 대상 테이블/컬럼:
--   - claim.category_id (FK → claim_categories.id) 제거
--   - claim.subcategory_id (FK → claim_subcategories.id) 제거
--   - claim_categories / claim_subcategories master 테이블 drop
--   - claim.claim_type1 / claim_type2 SF picklist 옵션값 직접 저장 컬럼 신규
--
-- 데이터 변환:
--   - dev DB cross-check (2026-05-13): claim 0건 / claim_categories 0건 / claim_subcategories 0건
--   - 변환 대상 데이터 부재 → 단순 컬럼 drop + 신규 컬럼 NOT NULL 직접 추가
--
-- SF describe 권위 출처:
--   - docs/plan/old_source_260408/sf-object-meta/_raw/DKRetail__Claim__c.json (restrictedPicklist: true)

-- 1. claim 테이블의 FK 컬럼 + 인덱스 + 제약 제거
ALTER TABLE powersales.claim DROP CONSTRAINT IF EXISTS claims_category_id_fkey;
ALTER TABLE powersales.claim DROP CONSTRAINT IF EXISTS claims_subcategory_id_fkey;
DROP INDEX IF EXISTS powersales.idx_claim_category_id;
DROP INDEX IF EXISTS powersales.idx_claim_subcategory_id;
ALTER TABLE powersales.claim DROP COLUMN IF EXISTS category_id;
ALTER TABLE powersales.claim DROP COLUMN IF EXISTS subcategory_id;

-- 2. claim 테이블에 enum value 컬럼 추가 (SF picklist 옵션값 직접 저장)
ALTER TABLE powersales.claim ADD COLUMN claim_type1 varchar(10) NOT NULL;
ALTER TABLE powersales.claim ADD COLUMN claim_type2 varchar(10) NOT NULL;

-- 3. claim_subcategories 테이블 drop (FK constraint → 테이블 → sequence)
ALTER TABLE IF EXISTS powersales.claim_subcategories DROP CONSTRAINT IF EXISTS claim_subcategories_category_id_fkey;
DROP INDEX IF EXISTS powersales.idx_claim_subcategories_category_id;
DROP TABLE IF EXISTS powersales.claim_subcategories;
DROP SEQUENCE IF EXISTS powersales.claim_subcategories_id_seq;

-- 4. claim_categories 테이블 drop
DROP TABLE IF EXISTS powersales.claim_categories;
DROP SEQUENCE IF EXISTS powersales.claim_categories_id_seq;
