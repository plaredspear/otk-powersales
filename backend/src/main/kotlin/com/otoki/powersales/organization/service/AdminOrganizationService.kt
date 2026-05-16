package com.otoki.powersales.organization.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.organization.dto.response.OrganizationResponse
import com.otoki.powersales.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminOrganizationService(
    private val organizationRepository: OrganizationRepository
) {

    /**
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     *              service 가 holder/ambient context 에 의존하지 않도록 explicit parameter 로 받는다.
     */
    fun getOrganizations(scope: DataScope, keyword: String?, level: String?): List<OrganizationResponse> {
        val branchCodes: List<String>? = when (val result = scope.effectiveBranchCodes(null)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyList()
        }

        return organizationRepository.searchForAdmin(
            keyword = keyword,
            level = level,
            branchCodes = branchCodes
        ).map { OrganizationResponse.from(it) }
    }
}
