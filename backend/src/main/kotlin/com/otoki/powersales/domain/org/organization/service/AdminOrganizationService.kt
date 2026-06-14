package com.otoki.powersales.domain.org.organization.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.org.organization.dto.response.OrganizationResponse
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminOrganizationService(
    private val organizationRepository: OrganizationRepository
) {

    /**
     * 조직마스터 목록 조회 — SF `CurrentUserBranchNameList.getOrgList()` (L32) 가시 범위 정합.
     *
     * 전사 권한(영업지원/관리자, `isAllBranches`)은 무제한, 그 외 사용자는 본인 HR 코드를
     * `OrgCodeLevel*` 트리에 매칭 + `OrgNameLevel3 IN (Retail/제1/CVS 사업부)` 제약
     * ([OrganizationRepository.searchForAdminByOrgTree]). 기존 `searchForAdmin` 의 `CostCenterLevel*`
     * 매칭(cost-center 시맨틱, AdminMonthlyIntegrationService 전용)과 분리.
     *
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     */
    fun getOrganizations(scope: DataScope, keyword: String?, level: String?): List<OrganizationResponse> {
        return when (val result = scope.effectiveBranchCodes(null)) {
            is EffectiveBranchResult.All ->
                // 전사 권한 — 사업부/트리 제약 없이 전체 조직 (keyword/level 만)
                organizationRepository.searchForAdminByOrgTree(keyword, level, orgTreeCodes = null)
            is EffectiveBranchResult.Filtered ->
                organizationRepository.searchForAdminByOrgTree(keyword, level, orgTreeCodes = result.codes)
            is EffectiveBranchResult.NoAccess -> emptyList()
        }.map { OrganizationResponse.from(it) }
    }
}
