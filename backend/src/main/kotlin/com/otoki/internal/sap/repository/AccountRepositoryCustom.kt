package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Account
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AccountRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        abcType: String?,
        branchCodes: List<String>?,
        accountStatusName: String?,
        pageable: Pageable
    ): Page<Account>
}
