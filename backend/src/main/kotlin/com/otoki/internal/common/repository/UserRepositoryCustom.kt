package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {

    fun findDistinctBranches(): List<BranchResponse>

    fun findAllEmployeeIds(): List<String>

    fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        appAuthority: String?,
        pageable: Pageable
    ): Page<User>
}
