package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.EmployeeListItem
import com.otoki.internal.admin.dto.response.EmployeeListResponse
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeeService(
    private val adminDataScopeService: AdminDataScopeService,
    private val userRepository: UserRepository
) {

    fun getEmployees(
        userId: Long,
        status: String?,
        costCenterCode: String?,
        keyword: String?,
        appAuthority: String?,
        page: Int,
        size: Int
    ): EmployeeListResponse {
        val scope = adminDataScopeService.resolve(userId)

        val effectiveBranchCodes: List<String>? = when {
            scope.isAllBranches && costCenterCode != null -> listOf(costCenterCode)
            scope.isAllBranches -> null
            costCenterCode != null -> {
                if (costCenterCode in scope.branchCodes) listOf(costCenterCode)
                else return emptyResponse(page, size)
            }
            else -> scope.branchCodes.ifEmpty { return emptyResponse(page, size) }
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
