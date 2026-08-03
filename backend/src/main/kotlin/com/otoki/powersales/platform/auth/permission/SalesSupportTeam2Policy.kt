package com.otoki.powersales.platform.auth.permission

/**
 * 영업지원2팀(조직코드 `4889`) 지점 스코프 정책 — **거래처 조회만 전 지점, 나머지는 본인 지점**.
 *
 * ## 배경
 * 영업지원2팀은 조직 계층상 `org_nm3 = "영업지원실"` / `org_nm4 = "영업지원2팀"` (Level5 부재 → Level4 코드 4889).
 * 이 때문에 과거 두 경로로 전사(전 지점) 조회가 열려 있었다.
 *  1. [com.otoki.powersales.user.service.UserRoleResolver.isSalesSupport] 의 `"영업지원"` 부분일치
 *     → `User.isSalesSupport = true` → 모든 지점 스코프 판정에서 전사.
 *  2. `costCenterCode == "4889"` 정확일치 전용 예외 (행사마스터 거래처 lookup / 행사사원 후보 lookup).
 *
 * 2026-08-02 에 두 경로를 모두 닫아 일반 지점 사용자와 동일한 스코프로 되돌렸고, 2026-08-03 요구로
 * **거래처 조회만** 다시 전 지점으로 연다 ([isAllBranchAccountLookup]). 영업지원1팀 / 영업본부는
 * 종전대로 전사 (SF `CurrentUserBranchNameList.cls` 의 `UserRole.Name LIKE '%영업지원%'` 정합 유지).
 *
 * ## 현재 스코프 요약
 * | 대상 | 스코프 |
 * |---|---|
 * | 거래처 관리 목록·상세·지점 셀렉터 (`GET /admin/accounts`, `/{id}`, `/branches`) | **전 지점** |
 * | 행사마스터 거래처 lookup (`GET /admin/accounts/lookup` + `/lookup-filter-options`) | **전 지점** |
 * | 그 외 전 화면 (대시보드 / 여사원 현황 / 행사사원 후보 / 진열 / 매출 등) | 본인 지점(4889) |
 * | 거래처 등록·수정·삭제 | 종전 권한 정책 그대로 (본 예외는 조회 전용) |
 *
 * 거래처 조회는 가시성 축이 둘이라 (지점 셀렉터/필터 = principal, sharing policy = [DataScope]) 진입점에서
 * 양쪽을 함께 열어야 한다 — 한쪽만 열면 셀렉터에는 지점이 보이는데 결과가 0건이 된다. 구현은
 * `AdminAccountController.accountScopePrincipal` / `accountDataScope` 참조.
 *
 * ## 지점 스코프 조회 시 자기 조직 노출
 * 지점 스코프 조회([com.otoki.powersales.domain.org.organization.repository.OrganizationRepositoryCustom.findTeamScheduleBranches]
 * 의 `allBranches = false`) 는 `OrgNameLevel3 IN ('Retail사업부','제1사업부','CVS사업부')` 화이트리스트를 거는데,
 * 영업지원2팀의 Level3 는 `"영업지원실"` 이라 이 필터에 걸리지 않아 결과가 0건이 된다. 따라서 해당 쿼리는
 * [ORG_CODE] 자기 조직을 화이트리스트 예외로 통과시켜 "본인 지점(영업지원2팀) 1개" 를 노출한다.
 *
 * 조직 개편으로 코드/명칭이 바뀌면 본 object 만 변경. web 대칭 상수는
 * `web/src/hooks/usePermission.ts` 의 `SALES_SUPPORT_TEAM2_COST_CENTER_CODE`.
 */
object SalesSupportTeam2Policy {

    /** 영업지원2팀 조직코드 — `Employee.costCenterCode` / `Organization.orgCodeLevel4` 축 (HR OrgCode). */
    const val ORG_CODE = "4889"

    /** 영업지원2팀 조직명 — `Organization.orgNameLevel4` / SF `UserRole.Name` 축. */
    const val ORG_NAME = "영업지원2팀"

    /** 로그인/대상 사용자가 영업지원2팀 소속인지 — `costCenterCode` 정확일치. null 이면 false. */
    fun isTeam2(costCenterCode: String?): Boolean = costCenterCode == ORG_CODE

    /**
     * 거래처 조회에서 지점 제한 없이 전 지점 거래처를 볼 수 있는 사용자인지
     * (2026-08-03 요구 — 영업지원2팀 한정 예외).
     *
     * 영업지원2팀은 전 지점의 행사를 대행 등록하므로, 본인 지점(4889) 소속 거래처만으로는 업무가
     * 성립하지 않는다. **거래처 조회 가시성만** 여는 예외이며(목록·상세·지점 셀렉터·행사마스터 lookup),
     * 등록/수정/삭제 권한과 다른 화면의 지점 스코프는 종전대로 본인 지점이다 (위 스코프 요약 표 참조).
     *
     * 판정 축을 [isTeam2] 와 분리해 둔 이유: "영업지원2팀인가" 라는 사실 판정과 "거래처 조회를
     * 열어줄 대상인가" 라는 정책 판정을 호출부에서 구분해 읽을 수 있게 하기 위함이다. 향후 대상이
     * 늘어나면 본 함수만 확장한다.
     */
    fun isAllBranchAccountLookup(costCenterCode: String?): Boolean = isTeam2(costCenterCode)
}
