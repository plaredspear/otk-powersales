-- Spec #318: Account 엔티티 미사용 컬럼 정리
-- 프로덕션에서 사용되지 않는 16개 컬럼 삭제
ALTER TABLE account
    DROP COLUMN IF EXISTS account_status_code,
    DROP COLUMN IF EXISTS email,
    DROP COLUMN IF EXISTS business_type,
    DROP COLUMN IF EXISTS business_category,
    DROP COLUMN IF EXISTS business_license_number,
    DROP COLUMN IF EXISTS division_code,
    DROP COLUMN IF EXISTS division_name,
    DROP COLUMN IF EXISTS sales_dept_code,
    DROP COLUMN IF EXISTS sales_dept_name,
    DROP COLUMN IF EXISTS consignment_acc,
    DROP COLUMN IF EXISTS werk1,
    DROP COLUMN IF EXISTS werk2,
    DROP COLUMN IF EXISTS werk3,
    DROP COLUMN IF EXISTS org_cd3,
    DROP COLUMN IF EXISTS org_cd4,
    DROP COLUMN IF EXISTS org_cd5;
