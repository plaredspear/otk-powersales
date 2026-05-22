package com.otoki.powersales.account.service

import com.otoki.powersales.account.dto.response.AccountListItem
import com.otoki.powersales.account.dto.response.AccountListResponse
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAccountService(
    private val accountRepository: AccountRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
) {

    /**
     * SF Sharing Rule 정책이 합성된 가시 Account 일람 (spec #782 P4-B).
     *
     * 본 메서드는 [SharingRulePolicyEvaluator.buildPredicate] 결과 Predicate 를
     * [AccountRepository.findAllAccessibleByPolicy] 에 전달. 우선순위 1-6 평가:
     * - Profile.viewAllData / PermissionSet.viewAllRecords[Account] → no-filter
     * - Owner / UserRole Hierarchy / SharingRule (Account 56 rule) / Legacy branchCodes OR 합성
     *
     * 기존 [getAccounts] 와 별도 진입점 — backward compat. 기존 메서드는 legacy `branchCodes` 차원만 사용.
     * 운영 안정화 시점에 [getAccounts] 본문도 본 패턴으로 통합 전환 가능.
     */
    fun getAccessibleAccountsByPolicy(scope: DataScope): List<Account> {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "Account",
            entityPath = account,
        )
        return accountRepository.findAllAccessibleByPolicy(policyPredicate)
    }

    /**
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     *              service 가 holder/ambient context 에 의존하지 않도록 explicit parameter 로 받는다.
     */
    fun getAccounts(
        scope: DataScope,
        keyword: String?,
        abcType: String?,
        branchCode: String?,
        accountStatusName: String?,
        page: Int,
        size: Int
    ): AccountListResponse {
        val effectiveBranchCodes: List<String>? = when (val result = scope.effectiveBranchCodes(branchCode)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyResponse(page, size)
        }

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val accountPage = accountRepository.searchForAdmin(
            keyword = keyword,
            abcType = abcType,
            branchCodes = effectiveBranchCodes,
            accountStatusName = accountStatusName,
            pageable = pageable
        )

        return AccountListResponse(
            content = accountPage.content.map { AccountListItem.from(it) },
            page = page,
            size = size,
            totalElements = accountPage.totalElements,
            totalPages = accountPage.totalPages
        )
    }

    private fun emptyResponse(page: Int, size: Int) = AccountListResponse(
        content = emptyList(),
        page = page,
        size = size,
        totalElements = 0,
        totalPages = 0
    )
}
