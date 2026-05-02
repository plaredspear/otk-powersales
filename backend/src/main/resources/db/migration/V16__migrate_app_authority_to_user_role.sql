-- Spec #573: appAuthority(한글) → role(enum.name 영문) 마이그레이션
-- - employee.app_authority(한글) → employee.role (enum.name)
-- - role_permission.role(한글) → role_permission.role(enum.name)
-- - 매칭되지 않는 한글 값:
--   - employee: NULL/빈 문자열 → NULL, 그 외 → 'UNKNOWN'
--   - role_permission: 매칭 실패 row 는 DELETE (운영 8개 역할 외에는 매트릭스 대상 아님)

-- 1. role_permission.role 컬럼 길이 확장 (HEADQUARTERS_MANAGER 20자 수용)
ALTER TABLE role_permission ALTER COLUMN role TYPE VARCHAR(50);

-- 2. role_permission.role 한글 → enum.name 일괄 UPDATE
UPDATE role_permission
SET role = CASE role
    WHEN '여사원'        THEN 'WOMAN'
    WHEN '조장'          THEN 'LEADER'
    WHEN '지점장'        THEN 'BRANCH_MANAGER'
    WHEN '영업부장'      THEN 'SALES_MANAGER'
    WHEN '사업부장'      THEN 'BUSINESS_MANAGER'
    WHEN '영업본부장'    THEN 'HEADQUARTERS_MANAGER'
    WHEN '영업지원실'    THEN 'SALES_SUPPORT'
    WHEN '시스템관리자'  THEN 'SYSTEM_ADMIN'
    ELSE role
END;

-- 3. role_permission 매칭되지 않는 row 정리 (UNKNOWN 등 운영 외 역할 제거)
DELETE FROM role_permission
WHERE role NOT IN (
    'WOMAN', 'LEADER', 'BRANCH_MANAGER', 'SALES_MANAGER',
    'BUSINESS_MANAGER', 'HEADQUARTERS_MANAGER', 'SALES_SUPPORT', 'SYSTEM_ADMIN'
);

-- 4. employee.app_authority 빈 문자열 → NULL
UPDATE employee SET app_authority = NULL WHERE app_authority = '';

-- 5. employee.app_authority 한글 → enum.name 일괄 UPDATE
UPDATE employee
SET app_authority = CASE app_authority
    WHEN '여사원'        THEN 'WOMAN'
    WHEN '조장'          THEN 'LEADER'
    WHEN '지점장'        THEN 'BRANCH_MANAGER'
    WHEN '영업부장'      THEN 'SALES_MANAGER'
    WHEN '사업부장'      THEN 'BUSINESS_MANAGER'
    WHEN '영업본부장'    THEN 'HEADQUARTERS_MANAGER'
    WHEN '영업지원실'    THEN 'SALES_SUPPORT'
    WHEN '시스템관리자'  THEN 'SYSTEM_ADMIN'
    ELSE app_authority
END;

-- 6. employee.app_authority 매칭되지 않는 값 → 'UNKNOWN'
UPDATE employee
SET app_authority = 'UNKNOWN'
WHERE app_authority IS NOT NULL
  AND app_authority NOT IN (
    'WOMAN', 'LEADER', 'BRANCH_MANAGER', 'SALES_MANAGER',
    'BUSINESS_MANAGER', 'HEADQUARTERS_MANAGER', 'SALES_SUPPORT', 'SYSTEM_ADMIN', 'UNKNOWN'
  );

-- 7. employee.app_authority → role RENAME + 길이 축소 (VARCHAR(50))
ALTER TABLE employee RENAME COLUMN app_authority TO role;
ALTER TABLE employee ALTER COLUMN role TYPE VARCHAR(50);
