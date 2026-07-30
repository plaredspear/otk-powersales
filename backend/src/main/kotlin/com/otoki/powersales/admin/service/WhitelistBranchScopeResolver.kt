package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
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
    /** 판정 통과 코드 → `BranchMapping` 확장 (레거시/별칭 조직코드로 적재된 행 누락 방지). */
    private val branchCodeExpander: BranchCodeExpander,
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
     * - 그 외(지점 사용자): [ReportBranchScopeService] 위임(본인 지점 스코프).
     *
     * 판정이 끝난 코드는 `BranchMapping` 으로 확장해 반환한다 — 반환값은 곧 조회 필터이고, 확장하지 않으면
     * 같은 지점이라도 조직 개편 전 코드로 적재된 행(예: 강북1지점 `5815` ← 구코드 `5452`)이 빠진다.
     * 이 화면들은 전사 권한자도 선택 없이 조회하면 `Filtered`(34개) 라 **전건 조회에서도** 누락됐다.
     * 판정 자체는 확장 전 원본 코드로 끝낸다([BranchCodeExpander] KDoc — 화이트리스트를 확장하면
     * 롤업 행이 권한 범위를 넓힌다). 확장으로 타 조직이 딸려오는 롤업 6건(`5829`/`5898`/`E5692`/`E5693`/
     * `E5721`/`5721`)은 34개 화이트리스트에 없어 전사 권한자 경로에서는 선택될 수 없다.
     */
    fun effectiveBranchCodes(principal: WebUserPrincipal, requestedBranchCode: String?): EffectiveBranchResult {
        if (!dashboardBranchResolver.isAllBranches(principal)) {
            return reportBranchScopeService.expandedEffectiveBranchCodes(principal, requestedBranchCode)
        }
        val requested = requestedBranchCode?.takeIf { it.isNotBlank() }
        return when {
            requested == null -> expanded(DashboardBranchResolver.WHITELIST_CODES.toList())
            requested in DashboardBranchResolver.WHITELIST_CODES -> expanded(listOf(requested))
            else -> EffectiveBranchResult.NoAccess
        }
    }

    private fun expanded(codes: List<String>): EffectiveBranchResult.Filtered =
        EffectiveBranchResult.Filtered(branchCodeExpander.expand(codes).toList())
}
