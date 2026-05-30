package com.otoki.powersales.organization.repository

import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.dto.OrganizationCacheDto

interface OrganizationRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization>

    /**
     * 조직마스터 페이지 가시 범위 조회 — SF `CurrentUserBranchNameList.getOrgList()` (L32) 정합.
     *
     * [searchForAdmin] 의 `branchCodes` 는 `CostCenterLevel*`(cc_cd*) 컬럼 매칭(cost-center 시맨틱,
     * [com.otoki.powersales.schedule.service.AdminMonthlyIntegrationService] 가 사용)인 반면, 조직마스터
     * 화면의 가시 범위는 사용자 HR 코드를 `OrgCodeLevel*`(org_cd*) 에 매칭하고 `OrgNameLevel3 IN
     * (Retail사업부/제1사업부/CVS사업부)` 사업부 제약을 건다 — SF getOrgList 와 동일 차원. 두 시맨틱이
     * 다른 컬럼이므로 별도 메서드로 분리 (cost-center 경로 mutate 금지).
     *
     * @param orgTreeCodes 사용자 HR 코드 (= `Employee.CostCenterCode__c`, 실제 값은 OrgCode). null = 무제한.
     */
    fun searchForAdminByOrgTree(
        keyword: String?,
        level: String?,
        orgTreeCodes: List<String>?
    ): List<Organization>

    fun expandCostCenterCodes(costCenterCodes: List<String>): List<String>

    /**
     * 전사 leaf level branch_codes 일괄 조회 (영업지원실 권한자용).
     *
     * SF `CurrentUserBranchNameList.getOrgList()` L35 정합 — `Org__c WHERE OrgNameLevel3__c='Retail사업부' AND OrgCodeLevel5__c IS NOT NULL OR OrgNameLevel3__c='제1사업부' OR 영업지원1팀/2팀` 분기의
     * 신규 단순화 매핑 — 전사 leaf cost_center_code (level5, 없으면 level4) distinct 합집합 (`is_deleted` 제외).
     */
    fun findAllLeafBranchCodes(): List<String>

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

    /**
     * cost_center 컬럼 cascade lookup — Level5 → Level4 순.
     *
     * 입력 `costCenterCode` 가 어느 레벨에 매칭될지 호출자가 모를 때 사용. Level5 단건 미존재 시
     * Level4 로 fallback. Level3 이하는 본 helper 에서 다루지 않는다 (호출자별 cascade 깊이가
     * 달라 정책 통합을 강제하지 않음 — Level3 까지 필요한 호출자는 [findFirstByOrgCodeCascade]
     * 시그니처 패턴 참고하여 별도 helper 추가).
     *
     * cascade 정책 (5→4 순서) 변경 시 본 메서드 1곳만 수정. 기존 호출자가 `findFirstByCostCenterLevel5/4`
     * 를 ?: chain 으로 직접 결합하던 패턴을 단일 호출로 응집.
     *
     * Redis 캐싱 — 결과는 [OrganizationCacheDto] 로 캐싱 (24h TTL, SAP daily sync 시 evict).
     * Organization entity 를 그대로 캐싱하지 않는 이유는 [OrganizationCacheDto] KDoc 참조.
     *
     * @return 매칭된 OrganizationCacheDto 또는 null (cascade 모두 miss)
     */
    fun findFirstByCostCenterCascade(costCenterCode: String): OrganizationCacheDto?

    /**
     * org_code 컬럼 cascade lookup — Level5 → Level4 → Level3 순.
     *
     * SF 레거시 `AppointmentTriggerHanlder.cls:264-271, 297-311` 의 OrgCode cascade 매칭 정합.
     * `EmployeeProfileResolver` / `UserRoleResolver` 등 사원 → 조직 매칭 로직에서 사용.
     *
     * cascade 정책 (5→4→3 순서) 변경 시 본 메서드 1곳만 수정.
     *
     * Redis 캐싱 — 결과는 [OrganizationCacheDto] 로 캐싱 (24h TTL, SAP daily sync 시 evict).
     *
     * @return 매칭된 OrganizationCacheDto 또는 null (cascade 모두 miss)
     */
    fun findFirstByOrgCodeCascade(orgCode: String): OrganizationCacheDto?
}
