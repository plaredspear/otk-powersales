package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.EffectiveBranchResult
import com.otoki.internal.admin.dto.response.AccountListItem
import com.otoki.internal.admin.dto.response.AccountListResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.repository.AccountRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAccountService(
    private val dataScopeHolder: DataScopeHolder,
    private val accountRepository: AccountRepository
) {

    fun getAccounts(
        keyword: String?,
        abcType: String?,
        branchCode: String?,
        accountStatusName: String?,
        page: Int,
        size: Int
    ): AccountListResponse {
        val scope = dataScopeHolder.require()

        val effectiveBranchCodes: List<String>? = when (val result = scope.effectiveBranchCodes(branchCode)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyResponse(page, size)
        }

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val accountPage = accountRepository.searchForAdmin(
            keyword = keyword,
            abcType = abcType,
            branchCodes = effectiveBranchCodes,
            accountStatusName = accountStatusName,
            pageable = pageable
        )

        return AccountListResponse(
            content = accountPage.content.map { AccountListItem.from(it) },
            page = page,
            size = size,
            totalElements = accountPage.totalElements,
            totalPages = accountPage.totalPages
        )
    }

    private fun emptyResponse(page: Int, size: Int) = AccountListResponse(
        content = emptyList(),
        page = page,
        size = size,
        totalElements = 0,
        totalPages = 0
    )
}
