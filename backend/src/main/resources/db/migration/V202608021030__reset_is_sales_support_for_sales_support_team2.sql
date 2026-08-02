-- 영업지원2팀(조직코드 4889) 전사(전 지점) 조회 예외 제거 (2026-08-02 요구).
--
-- UserRoleResolver.isSalesSupport 가 조직명 "영업지원" 부분일치로 산출하던 탓에 영업지원2팀 소속
-- 사용자도 User.is_sales_support = true 로 적재돼 모든 지점 스코프 판정에서 전사가 열려 있었다.
-- 산출 로직에서 영업지원2팀을 제외했으나 is_sales_support 는 캐시 컬럼(적재/SAP 인사발령 sync 시점 산출)
-- 이라, 이미 true 로 적재된 기존 row 를 여기서 1회 정정한다.
--
-- 이후 재산출 경로(AppointmentUserProfileUpdater / UserProvisioningService)는 새 로직을 타므로
-- 본 마이그레이션이 되돌려지지 않는다. 영업지원1팀 / 영업본부는 대상이 아니다.
UPDATE "user" u
SET is_sales_support = false
WHERE COALESCE(u.is_sales_support, false) = true
  AND (
      u.cost_center_code = '4889'
      OR EXISTS (
          SELECT 1
          FROM employee e
          WHERE e.employee_code = u.employee_code
            AND e.cost_center_code = '4889'
      )
  );
