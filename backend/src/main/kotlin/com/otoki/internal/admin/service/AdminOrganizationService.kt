package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.EffectiveBranchResult
import com.otoki.internal.admin.dto.response.OrganizationResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminOrganizationService(
    private val dataScopeHolder: DataScopeHolder,
    private val organizationRepository: OrganizationRepository
) {

    fun getOrganizations(keyword: String?, level: String?): List<OrganizationResponse> {
        val scope = dataScopeHolder.require()

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
