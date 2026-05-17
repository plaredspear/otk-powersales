package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.AccountCategoryMaster
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCategoryMasterRepository : JpaRepository<AccountCategoryMaster, Long> {
    fun findByAccountCode(accountCode: String): AccountCategoryMaster?

    fun findByUseSearchTrueAndIsDeletedNotOrderByAccountCode(isDeleted: Boolean): List<AccountCategoryMaster>
}
