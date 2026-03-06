package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse

interface UserRepositoryCustom {

    fun findDistinctBranches(): List<BranchResponse>

    fun findAllEmployeeIds(): List<String>
}
