package com.otoki.powersales.organization.repository

import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.organization.entity.Organization

interface OrganizationRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization>

    fun expandCostCenterCodes(costCenterCodes: List<String>): List<String>

    fun findFirstByAnyOrgCodeLevel(orgCode: String): Organization?

    /**
     * 여사원 일정관리 지점 드롭다운용 조직 목록.
     *
     * SF 레거시 `CurrentUserBranchNameList.getOrgList()` 정합:
     * - `allBranches = false`: 본인 cost center(`hrCode`)가 속한 조직 트리 + `OrgNameLevel3 IN ('Retail사업부','제1사업부','CVS사업부')`
     * - `allBranches = true`: `OrgNameLevel3 IN ('Retail사업부','제1사업부')` 또는 `OrgNameLevel4 IN ('영업지원1팀','영업지원2팀')` (CVS 미포함)
     *
     * branchCode/branchName 조합 — Level5 있으면 `(cc_cd5, org_nm5)`, 없으면 `(cc_cd4, org_nm4)`.
     */
    fun findTeamScheduleBranches(hrCode: String?, allBranches: Boolean): List<BranchResponse>

    /**
     * 관리자(SYSTEM_ADMIN 등) 전용 — 삭제되지 않은 전체 조직에서 지점 드롭다운 옵션 추출.
     * Level5 있으면 `(cc_cd5, org_nm5)`, 없으면 `(cc_cd4, org_nm4)`.
     */
    fun findAllTeamScheduleBranches(): List<BranchResponse>
}
