package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 대시보드(투입현황) "지점 선택" 드롭다운 옵션 결정 — 대시보드 전용.
 *
 * 여사원 일정 등과 공유하는 [WomenScheduleBranchResolver] 와 달리, 대시보드는 전사 권한자에게
 * **고정된 지점 화이트리스트**([DASHBOARD_ALL_BRANCHES])만 노출한다. 이는 SF 레거시
 * `CurrentUserBranchNameList.getOrgList()` 의 전사 분기(제1사업부 전체 + 영업지원1·2팀 포함)와
 * 다른 대시보드 전용 규칙(deviation)으로, 운영에서 대시보드에 표기할 지점을
 * Retail사업부 32개 지점 + 영업지원2팀 + CVS전략팀 = 34개로 한정한다.
 *
 * 권한 분기:
 * - 전사 권한자 (SYSTEM_ADMIN / 영업지원 / 본부장·사업부장·영업부장): [DASHBOARD_ALL_BRANCHES] 34개 고정.
 * - 그 외 (본인 지점 스코프): [WomenScheduleBranchResolver] 위임 — 본인 소속 지점만.
 *
 * 하드코딩 사유: 34개 목록이 조직 개편과 무관하게 대시보드에 고정 노출되어야 한다는 운영 요구.
 * 조직 코드는 `Organization.orgCodeLevel5`(영업지원2팀은 Level5 부재로 Level4 `4889`) 기준.
 */
@Component
class DashboardBranchResolver(
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {
    /** SF "전 지점 가시" Profile.Name 집합 — [WomenScheduleBranchResolver] 와 동일 기준. */
    private val allBranchesProfiles: Set<String> = setOf("1.본부장", "2.사업부장", "3.영업부장")

    /** 전사 권한자 판정 — 셀렉터/조회 스코프 모두 이 기준으로 34개 화이트리스트를 고정한다. */
    fun isAllBranches(principal: WebUserPrincipal): Boolean {
        val profileName = principal.profileName
        return profileName == SYSTEM_ADMIN_PROFILE_NAME ||
            principal.isSalesSupport ||
            profileName in allBranchesProfiles
    }

    fun resolveBranches(principal: WebUserPrincipal): List<BranchResponse> {
        return if (isAllBranches(principal)) {
            DASHBOARD_ALL_BRANCHES
        } else {
            womenScheduleBranchResolver.resolveBranches(principal)
        }
    }

    /**
     * 대시보드 조회 지점 스코프 산출 — 셀렉터([resolveBranches]) 와 동일하게 34개 화이트리스트로 제한.
     *
     * - 전사 권한자:
     *   - 선택값 없음 → 34개 코드 전체(Filtered). (전건 조회가 아니라 34개 지점으로 제한)
     *   - 선택값이 34개 안 → 그 지점(Filtered).
     *   - 선택값이 34개 밖 → NoAccess(차단, 34개 밖 지점 조회 방어).
     * - 그 외(지점 사용자): [DataScope.effectiveBranchCodes] 그대로(본인 지점 스코프).
     */
    fun effectiveBranchCodes(
        principal: WebUserPrincipal,
        scope: DataScope,
        requestedBranchCode: String?,
    ): EffectiveBranchResult {
        if (!isAllBranches(principal)) {
            return scope.effectiveBranchCodes(requestedBranchCode?.takeIf { it.isNotBlank() })
        }
        val requested = requestedBranchCode?.takeIf { it.isNotBlank() }
        return when {
            requested == null -> EffectiveBranchResult.Filtered(WHITELIST_CODES.toList())
            requested in WHITELIST_CODES -> EffectiveBranchResult.Filtered(listOf(requested))
            else -> EffectiveBranchResult.NoAccess
        }
    }

    companion object {
        /**
         * 대시보드 전사 권한자 지점 화이트리스트 — Retail사업부 32개 지점 + 영업지원2팀 + CVS전략팀.
         * 코드는 `Organization.orgCodeLevel5`(영업지원2팀만 Level5 부재로 Level4 `4889`) 기준,
         * `org_cd3 → org_cd4 → org_cd5` 오름차순 정렬 (기존 지점 드롭다운 정렬 규칙 정합).
         */
        val DASHBOARD_ALL_BRANCHES: List<BranchResponse> = listOf(
            // 영업지원실 - 영업지원2팀 (Level5 부재 → Level4 코드 4889)
            BranchResponse("4889", "영업지원2팀"),
            // CVS사업부 - CVS전략팀
            BranchResponse("5694", "CVS전략팀"),
            // Retail사업부 1영업부
            BranchResponse("5815", "강북1지점"),
            BranchResponse("5816", "강북4지점"),
            BranchResponse("5817", "강남1지점"),
            BranchResponse("5818", "강북3지점"),
            BranchResponse("5819", "강북2지점"),
            BranchResponse("5820", "강북5지점"),
            // Retail사업부 2영업부
            BranchResponse("5822", "강남4지점"),
            BranchResponse("5823", "강남3지점"),
            BranchResponse("5824", "강남2지점"),
            // Retail사업부 3영업부
            BranchResponse("5826", "인천1지점"),
            BranchResponse("5827", "인천2지점"),
            BranchResponse("5828", "인천3지점"),
            // Retail사업부 4영업부
            BranchResponse("5830", "경기1지점"),
            BranchResponse("5831", "경기2지점"),
            BranchResponse("5832", "원주1지점"),
            BranchResponse("5833", "경기3지점"),
            BranchResponse("5834", "강릉지점"),
            // Retail사업부 5영업부
            BranchResponse("5836", "대전1지점"),
            BranchResponse("5837", "천안1지점"),
            BranchResponse("5838", "청주1지점"),
            // Retail사업부 6영업부
            BranchResponse("5840", "광주1지점"),
            BranchResponse("5841", "전주1지점"),
            BranchResponse("5842", "제주지점"),
            BranchResponse("5843", "순천지점"),
            // Retail사업부 7영업부
            BranchResponse("5845", "대구1지점"),
            BranchResponse("5846", "대구2지점"),
            BranchResponse("5847", "대구3지점"),
            // Retail사업부 8영업부
            BranchResponse("5849", "부산1지점"),
            BranchResponse("5850", "울산1지점"),
            BranchResponse("5851", "창원1지점"),
            BranchResponse("5852", "부산2지점"),
            BranchResponse("5853", "진주1지점"),
        )

        /** [DASHBOARD_ALL_BRANCHES] 지점 코드 집합 — 조회 스코프 제한 / IDOR 검증용. */
        val WHITELIST_CODES: Set<String> = DASHBOARD_ALL_BRANCHES.map { it.branchCode }.toSet()
    }
}
