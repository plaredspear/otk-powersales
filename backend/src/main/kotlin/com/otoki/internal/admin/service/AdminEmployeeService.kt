package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.EffectiveBranchResult
import com.otoki.internal.admin.dto.response.EmployeeListItem
import com.otoki.internal.admin.dto.response.EmployeeListResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeeService(
    private val dataScopeHolder: DataScopeHolder,
    private val userRepository: UserRepository
) {

    fun getEmployees(
        status: String?,
        costCenterCode: String?,
        keyword: String?,
        appAuthority: String?,
        page: Int,
        size: Int
    ): EmployeeListResponse {
        val scope = dataScopeHolder.require()

        val effectiveBranchCodes: List<String>? = when (val result = scope.effectiveBranchCodes(costCenterCode)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyResponse(page, size)
        }

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val userPage = userRepository.findEmployees(status, effectiveBranchCodes, keyword, appAuthority, pageable)

        return EmployeeListResponse(
            content = userPage.content.map { EmployeeListItem.from(it) },
            page = page,
            size = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    private fun emptyResponse(page: Int, size: Int) = EmployeeListResponse(
        content = emptyList(),
        page = page,
        size = size,
        totalElements = 0,
        totalPages = 0
    )
}
