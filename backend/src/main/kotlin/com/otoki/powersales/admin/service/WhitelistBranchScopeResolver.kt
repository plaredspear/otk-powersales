package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 관리자 목록 화면 "지점별 조회" 지점 결정 — 대시보드 고정 화이트리스트 정합 공용 리졸버.
 *
 * 대시보드([DashboardBranchResolver]) 와 동일하게 전사 권한자에게 **고정 지점 화이트리스트**
 * ([DashboardBranchResolver.DASHBOARD_ALL_BRANCHES] 34개 — Retail 32 + 영업지원2팀 + CVS전략팀) 만
 * 노출·조회하도록 제한한다. 셀렉터 + 목록/엑셀 조회 스코프 양쪽 모두 34개로 제한한다(운영 요구).
 *
 * 대시보드 자신은 셀렉터/조회 위임 대상이 [WomenScheduleBranchResolver] + [com.otoki.powersales.admin.dto.DataScope]
 * 라 [DashboardBranchResolver] 가 별도로 그 위임을 담당하고, 이 공용 리졸버는 위임 대상이
 * [ReportBranchScopeService] 로 동일한 화면들(행사마스터·진열스케줄마스터 등)이 공유한다.
 *
 * 권한 분기 (전사 권한자 판정은 [DashboardBranchResolver.isAllBranches] 재사용 — 대시보드와 동일 기준):
 * - 전사 권한자 (SYSTEM_ADMIN / 영업지원 / 본부장·사업부장·영업부장):
 *   - 셀렉터([getBranches]): 34개 고정.
 *   - 조회 스코프([effectiveBranchCodes]): 선택 없으면 34개 코드 전체, 선택 시 그 지점(단, 34개 밖이면 차단).
 * - 그 외 (본인 지점 스코프): [ReportBranchScopeService] 위임 — 본인 소속 지점만(기존과 동일).
 */
@Component
class WhitelistBranchScopeResolver(
    private val reportBranchScopeService: ReportBranchScopeService,
    private val dashboardBranchResolver: DashboardBranchResolver,
) {
    /**
     * 목록 화면 지점 셀렉터 옵션.
     * - 전사 권한자: [DashboardBranchResolver.DASHBOARD_ALL_BRANCHES] 34개 고정.
     * - 그 외: 본인 지점 1건 ([ReportBranchScopeService.getBranches]).
     */
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> {
        return if (dashboardBranchResolver.isAllBranches(principal)) {
            DashboardBranchResolver.DASHBOARD_ALL_BRANCHES
        } else {
            reportBranchScopeService.getBranches(principal)
        }
    }

    /**
     * 목록/엑셀 조회 지점 스코프 산출.
     *
     * - 전사 권한자:
     *   - 선택값 없음 → 34개 코드 전체(Filtered) — 셀렉터·조회 모두 34개로 제한.
     *   - 선택값이 34개 안 → 그 지점(Filtered).
     *   - 선택값이 34개 밖 → NoAccess(차단, IDOR 방어).
     * - 그 외(지점 사용자): [ReportBranchScopeService.effectiveBranchCodes] 그대로(본인 지점 스코프).
     */
    fun effectiveBranchCodes(principal: WebUserPrincipal, requestedBranchCode: String?): EffectiveBranchResult {
        if (!dashboardBranchResolver.isAllBranches(principal)) {
            return reportBranchScopeService.effectiveBranchCodes(principal, requestedBranchCode)
        }
        val requested = requestedBranchCode?.takeIf { it.isNotBlank() }
        return when {
            requested == null -> EffectiveBranchResult.Filtered(DashboardBranchResolver.WHITELIST_CODES.toList())
            requested in DashboardBranchResolver.WHITELIST_CODES -> EffectiveBranchResult.Filtered(listOf(requested))
            else -> EffectiveBranchResult.NoAccess
        }
    }
}
