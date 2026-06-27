package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
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

    /**
     * 거래처유형(유통형태) 검색용 — 이름 부분일치 + 조회화면이용(useSearch) + 미삭제.
     *
     * 화면 검색어로 매칭되는 거래처유형마스터 Name 목록을 얻어, 호출 측에서 `AccountType` 으로
     * 환원해 `Account.accountType` IN 조건을 구성한다. `getSearchCategories` 와 동일하게
     * useSearch=true 항목만 검색 대상으로 노출.
     */
    fun findByNameContainingIgnoreCaseAndUseSearchTrueAndIsDeletedNot(
        name: String,
        isDeleted: Boolean,
    ): List<AccountCategoryMaster>
}
