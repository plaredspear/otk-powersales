package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
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
