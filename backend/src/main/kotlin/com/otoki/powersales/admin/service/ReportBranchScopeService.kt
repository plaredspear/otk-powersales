package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 보고서 그룹 공용 지점 스코프 리졸버.
 *
 * ## 책임
 * 보고서 각 화면의 "지점별 조회" 조건을 위한 공통 로직. 세 가지를 제공한다:
 * - [getBranches] : 화면 지점 셀렉터의 옵션(현재 사용자가 조회 가능한 지점 화이트리스트).
 * - [effectiveBranchCodes] : 조회 시 실제 적용할 지점 코드 산출(선택값 IDOR 검증 포함).
 * - [expandedEffectiveBranchCodes] : 위 결과에 `BranchMapping` 확장을 얹은 조회 필터용 코드.
 *
 * ## 지점 판정 기준 (costCenterCode — 사원 소속 지점)
 * [AdminDataScopeService.resolve] 의 [com.otoki.powersales.admin.dto.DataScope] 를 그대로 사용한다.
 * - 전사 권한자(시스템 관리자 / 영업지원 / 본부장·사업부장·영업부장): 전 지점 노출, 선택 시 그 지점으로 좁힘.
 * - 그 외(조장·지점장·여사원 등): 본인 `costCenterCode` 단일 지점. 선택 파라미터가 본인 지점 밖이면 무시(IDOR 차단).
 * 여사원 일정용 조직 트리 확장(형제 지점 딸림)은 쓰지 않아 본인 지점만 노출된다(현장점검 테마 getBranches 와 동일 규칙).
 *
 * ## 레거시 대비
 * 레거시 SF 보고서(Report scope=organization)는 전사 노출이었다. 본 지점 스코프 도입은 관리 편의를 위한 신규 정책
 * (deviation) 이며, 전사 권한자는 종전대로 전건을 볼 수 있다.
 */
@Service
@Transactional(readOnly = true)
class ReportBranchScopeService(
    private val dataScopeService: AdminDataScopeService,
    private val organizationRepository: OrganizationRepository,
    private val branchCodeExpander: BranchCodeExpander,
) {

    /**
     * 화면 지점 셀렉터 옵션 조회.
     *
     * 전사 권한자는 근무형태별 여사원인원현황·대시보드와 동일한 고정 지점 화이트리스트
     * ([DashboardBranchResolver.DASHBOARD_ALL_BRANCHES] 34개), 그 외는 본인 `costCenterCode` 단일 지점 1건.
     * costCenterCode 로 조직을 찾아 Level5(지점) 우선, 없으면 Level4 이름을 표시명으로 쓴다.
     * 목록이 비면(권한 지점 없음) 빈 리스트 — 프론트는 응답 길이로 단일/다중을 판별한다.
     *
     * 전사 목록을 34개로 좁힌 이유: 종전 `Organization` 전건 조회는 Level5(지점) 부재 시 Level4(팀) 로
     * fallback 해 `FS마케팅1팀` 같은 팀 단위 조직까지 섞어 노출했고, 같은 지점을 고르는 다른 화면들과
     * 목록이 달랐다. 셀렉터 목록만 좁히며 조회 스코프는 [effectiveBranchCodes] 기준 그대로다
     * (전사 권한자가 지점을 고르지 않으면 종전처럼 전건).
     */
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val scope = dataScopeService.resolve(principal)
        if (scope.isAllBranches) {
            return DashboardBranchResolver.DASHBOARD_ALL_BRANCHES
        }
        val code = principal.costCenterCode?.takeIf { it.isNotBlank() } ?: return emptyList()
        val org = organizationRepository.findFirstByAnyOrgCodeLevel(code) ?: return emptyList()
        val name = org.orgNameLevel5?.takeIf { it.isNotBlank() } ?: org.orgNameLevel4 ?: return emptyList()
        return listOf(BranchResponse(branchCode = code, branchName = name))
    }

    /**
     * 조회 지점 스코프 산출.
     *
     * [com.otoki.powersales.admin.dto.DataScope.effectiveBranchCodes] 로 위임한다:
     * - 전사 권한자 + 선택값 있음 → 그 지점으로 좁힘(Filtered).
     * - 전사 권한자 + 선택값 없음 → 전건(All).
     * - 지점 사용자 + 선택값이 본인 지점 → 그 지점(Filtered). 밖이면 본인 지점 전체(IDOR 차단).
     * - 지점 사용자 + 선택값 없음 → 본인 지점 전체(Filtered). 권한 지점이 없으면 NoAccess.
     */
    fun effectiveBranchCodes(principal: WebUserPrincipal, requestedBranchCode: String?): EffectiveBranchResult {
        return dataScopeService.resolve(principal).effectiveBranchCodes(requestedBranchCode?.takeIf { it.isNotBlank() })
    }

    /**
     * 조회 필터용 지점 코드 산출 — [effectiveBranchCodes] 결과에 [BranchCodeExpander] 확장을 얹는다.
     *
     * 권한 판정은 [effectiveBranchCodes] 가 **확장 전 원본 코드**로 끝낸 뒤, 통과한 코드만 넓혀 조회 필터로 쓴다
     * ([BranchCodeExpander] KDoc — 화이트리스트 자체를 확장하면 롤업 행이 권한 범위를 넓힌다).
     * 레거시/별칭 조직코드로 적재된 데이터가 지점 선택 시 누락되지 않게 하려는 것으로, 근무형태별
     * 여사원인원현황·전문행사조 확정 인원 등 기존 화면과 동일한 순서다.
     *
     * 조회 필터로 쓰는 호출부는 모두 이쪽을 쓴다 — [effectiveBranchCodes] 는 확장 전 원본(=권한 판정 결과) 을
     * 노출하는 단계로 남겨 두 축(판정 / 조회 필터) 이 코드에서 구분되게 한다.
     */
    fun expandedEffectiveBranchCodes(principal: WebUserPrincipal, requestedBranchCode: String?): EffectiveBranchResult {
        return when (val result = effectiveBranchCodes(principal, requestedBranchCode)) {
            is EffectiveBranchResult.Filtered -> EffectiveBranchResult.Filtered(
                branchCodeExpander.expand(result.codes).toList(),
            )
            else -> result
        }
    }
}
