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

    /** 권한별 조회 허용 지점 목록. */
    fun resolveBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val profileName = principal.profileName
        val isAllBranches = principal.isSalesSupport || profileName in allBranchesProfiles
        return when {
            profileName == SYSTEM_ADMIN_PROFILE_NAME -> organizationRepository.findAllTeamScheduleBranches()
            isAllBranches -> organizationRepository.findTeamScheduleBranches(hrCode = null, allBranches = true)
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
