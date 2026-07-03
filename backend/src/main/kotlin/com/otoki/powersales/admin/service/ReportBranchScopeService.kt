package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 보고서 그룹 공용 지점 스코프 리졸버.
 *
 * ## 책임
 * 보고서 각 화면의 "지점별 조회" 조건을 위한 공통 로직. 두 가지를 제공한다:
 * - [getBranches] : 화면 지점 셀렉터의 옵션(현재 사용자가 조회 가능한 지점 화이트리스트).
 * - [effectiveBranchCodes] : 조회 시 실제 적용할 지점 코드 산출(선택값 IDOR 검증 포함).
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
) {

    /**
     * 화면 지점 셀렉터 옵션 조회.
     *
     * 전사 권한자는 전 지점([OrganizationRepository.findAllTeamScheduleBranches]), 그 외는 본인 `costCenterCode`
     * 단일 지점 1건. costCenterCode 로 조직을 찾아 Level5(지점) 우선, 없으면 Level4 이름을 표시명으로 쓴다.
     * 목록이 비면(권한 지점 없음) 빈 리스트 — 프론트는 응답 길이로 단일/다중을 판별한다.
     */
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val scope = dataScopeService.resolve(principal)
        if (scope.isAllBranches) {
            return organizationRepository.findAllTeamScheduleBranches()
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
}
