package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.AccountCategoryMaster
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCategoryMasterRepository : JpaRepository<AccountCategoryMaster, Long> {
    fun findByAccountCode(accountCode: String): AccountCategoryMaster?
}
