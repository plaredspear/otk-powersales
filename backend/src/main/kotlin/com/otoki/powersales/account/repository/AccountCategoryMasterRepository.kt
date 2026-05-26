package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.AccountCategoryMaster
import org.springframework.data.jpa.repository.JpaRepository

interface AccountCategoryMasterRepository : JpaRepository<AccountCategoryMaster, Long> {
    fun findByAccountCode(accountCode: String): AccountCategoryMaster?

    fun findByUseSearchTrueAndIsDeletedNotOrderByAccountCode(isDeleted: Boolean): List<AccountCategoryMaster>

    /**
     * spec #680 §5.3 — refreshIntegration 의 EmployeeInputCriteriaMaster lookup.
     *
     * `Account.accountType.displayName` (예: "슈퍼") 으로 매칭되는 AccountCategoryMaster 조회.
     * legacy `MonthlyEmpIntegrationSchTriggerHandler` 의 `criteriaMap key = Category.Name + Year + Month`
     * 동등 — Category.Name 매칭. 운영 정합 dev 검증 2026-05-26.
     */
    fun findByName(name: String): AccountCategoryMaster?
}
