-- Spec #807 — UserRoleEnum destructive 폐기 사전 알림.
-- enum.name 값 (WOMAN/LEADER/BRANCH_MANAGER/ACCOUNT_VIEW_ALL/SALES_MANAGER/BUSINESS_MANAGER/
-- HEADQUARTERS_MANAGER/SALES_SUPPORT/SYSTEM_ADMIN/UNKNOWN) 이 남아있으면 application 단의
-- AppAuthority 상수 (SF picklist value "여사원"/"조장"/"지점장"/"AccountViewAll") 비교에서 불일치.
-- 사용자 결정 (2026-05-23): 기존 데이터 무시 + 신규 등록. 본 검증은 NOTICE 알림 only.
DO $$
DECLARE
    enum_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO enum_count FROM powersales.employee
    WHERE role IN ('WOMAN', 'LEADER', 'BRANCH_MANAGER', 'ACCOUNT_VIEW_ALL', 'SALES_MANAGER',
                   'BUSINESS_MANAGER', 'HEADQUARTERS_MANAGER', 'SALES_SUPPORT', 'SYSTEM_ADMIN', 'UNKNOWN');
    IF enum_count > 0 THEN
        RAISE NOTICE 'spec #807 알림: Employee.role enum.name 값 % 건 보유. cut-over 시점 신규 등록 필요.', enum_count;
    END IF;
END $$;
