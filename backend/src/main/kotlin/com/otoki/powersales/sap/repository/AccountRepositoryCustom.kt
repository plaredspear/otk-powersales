package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.Account
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
