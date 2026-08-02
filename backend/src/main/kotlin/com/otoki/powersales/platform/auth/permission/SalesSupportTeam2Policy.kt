package com.otoki.powersales.platform.auth.permission

/**
 * 영업지원2팀(조직코드 `4889`) 지점 스코프 정책 — **일반 지점과 동일하게 본인 지점만** (2026-08-02 요구).
 *
 * ## 배경
 * 영업지원2팀은 조직 계층상 `org_nm3 = "영업지원실"` / `org_nm4 = "영업지원2팀"` (Level5 부재 → Level4 코드 4889).
 * 이 때문에 과거 두 경로로 전사(전 지점) 조회가 열려 있었다.
 *  1. [com.otoki.powersales.user.service.UserRoleResolver.isSalesSupport] 의 `"영업지원"` 부분일치
 *     → `User.isSalesSupport = true` → 모든 지점 스코프 판정에서 전사.
 *  2. `costCenterCode == "4889"` 정확일치 전용 예외 (행사마스터 거래처 lookup / 행사사원 후보 lookup).
 *
 * 두 경로를 모두 닫아 영업지원2팀을 일반 지점 사용자와 동일한 스코프로 되돌린다. 영업지원1팀 / 영업본부는
 * 종전대로 전사 (SF `CurrentUserBranchNameList.cls` 의 `UserRole.Name LIKE '%영업지원%'` 정합 유지).
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
}
