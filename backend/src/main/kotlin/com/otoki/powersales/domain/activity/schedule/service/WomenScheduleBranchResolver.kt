package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 여사원 일정/근무 도메인의 "지점 선택" 옵션 결정 — SF 레거시
 * `ScheduleSearchByTeamMemberController.init()` → `CurrentUserBranchNameList.getOrgList()` 정합.
 *
 * 여사원 일정관리(AdminTeamScheduleService.getBranches)와 근무기간 조회(AdminAttendInfoService)가
 * 동일한 권한별 지점 스코프를 공유하기 위한 단일 출처. 권한 분기:
 * - SYSTEM_ADMIN: Organization 전체 distinct(Level5 우선)
 * - ALL_BRANCHES (영업지원/본부): RT.Name in ('영업지원실','영업본부') (CVS 미포함)
 * - 그 외 (LEADER/BRANCH_MANAGER/WOMAN 등): 본인 costCenterCode 기준 조직 트리 + Retail/제1/CVS사업부
 *
 * [resolveBranches] 가 반환하는 목록은 곧 그 사용자가 조회 허용된 지점 화이트리스트이며,
 * [assertBranchAllowed] 로 임의 branchCode 조회(IDOR) 를 차단한다.
 */
@Component
class WomenScheduleBranchResolver(
    private val organizationRepository: OrganizationRepository,
) {
    /** SF "전 지점 가시" Profile.Name 집합 — 영업본부 / 사업부장 등. SF AppointmentTriggerHanlder.cls:344-365 정합. */
    private val allBranchesProfiles: Set<String> = setOf("1.본부장", "2.사업부장", "3.영업부장")

    companion object {
        /**
         * 지점 스코프와 무관하게 전체 지점(전사) 조회를 허용하는 조직코드.
         * 영업지원2팀(costCenterCode = 4889, org_nm3="영업지원실" / org_nm4="영업지원2팀") 소속 사용자는
         * 시스템 관리자와 동일하게 전사 데이터를 조회한다(2026-07-14 요구, 행사사원 후보 lookup 과 동일 정책).
         * 조직 개편으로 코드가 바뀌면 본 상수만 변경.
         */
        const val ALL_BRANCH_LOOKUP_COST_CENTER_CODE = "4889"
    }

    /**
     * 로그인 사용자가 지점 스코프를 건너뛰고 전사 조회를 허용받는 대상인지 여부.
     * SYSTEM_ADMIN 은 [resolveBranches] 에서 이미 전체 지점을 반환하므로, 본 판정은
     * 그 외 프로필이라도 영업지원2팀([ALL_BRANCH_LOOKUP_COST_CENTER_CODE]) 이면 true.
     * 대행 로그인 시에도 principal 의 costCenterCode(= 대행 대상 기준)로 판정한다.
     */
    fun isAllBranchLookupUser(principal: WebUserPrincipal): Boolean =
        principal.costCenterCode == ALL_BRANCH_LOOKUP_COST_CENTER_CODE

    /**
     * 지점 보안 필터를 적용하지 않는 "전사 가시" 사용자인지 여부 — [resolveBranches] 의 전사 분기와 **동일 술어**.
     *
     * 목록/엑셀 조회 스코프를 셀렉터 옵션([resolveBranches]) 과 동일 출처로 맞추려는 호출부가
     * `DataScope.isAllBranches` 를 결정할 때 사용한다 (전사 판정 로직을 호출부에 복제하지 않도록 노출).
     *
     * **주의**: [isAllBranchLookupUser] (영업지원2팀 4889) 는 여기에 포함하지 않는다 — [resolveBranches]
     * 의 전사 분기에 없는 조건을 넣으면 셀렉터는 본인 조직 트리만 주는데 조회는 전사가 되어
     * fail-open 방향으로 두 축이 갈라진다. 4889 를 여사원 현황에서도 전사로 취급해야 한다면
     * [resolveBranches] 의 전사 분기와 **함께** 넓혀야 한다 (셀렉터 UX 까지 영향).
     */
    fun isAllBranchesUser(principal: WebUserPrincipal): Boolean = isAllBranchesScope(principal)

    /**
     * [resolveBranches] 전사 분기 / [isAllBranchesUser] 가 공유하는 단일 술어.
     * 두 함수가 구조적으로 갈라질 수 없도록 판정을 한 곳에 둔다.
     */
    private fun isAllBranchesScope(principal: WebUserPrincipal): Boolean {
        val profileName = principal.profileName
        return profileName == SYSTEM_ADMIN_PROFILE_NAME ||
            principal.isSalesSupport ||
            profileName in allBranchesProfiles
    }

    /** 권한별 조회 허용 지점 목록. */
    fun resolveBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val profileName = principal.profileName
        return when {
            profileName == SYSTEM_ADMIN_PROFILE_NAME -> organizationRepository.findAllTeamScheduleBranches()
            isAllBranchesScope(principal) ->
                organizationRepository.findTeamScheduleBranches(hrCode = null, allBranches = true)
            else -> organizationRepository.findTeamScheduleBranches(
                hrCode = principal.costCenterCode,
                allBranches = false,
            )
        }
    }

    /**
     * 요청 branchCode 가 사용자 권한 스코프에 포함되는지 검증. 미포함이면 false.
     * 호출부는 false 시 빈 결과를 반환하거나 예외를 던져 IDOR 을 차단한다.
     */
    fun isBranchAllowed(principal: WebUserPrincipal, branchCode: String): Boolean =
        resolveBranches(principal).any { it.branchCode == branchCode }
}
