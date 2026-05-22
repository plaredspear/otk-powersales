package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.EntityPathBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AdminAccountService.getAccessibleAccountsByPolicy Service layer 통합 검증 (spec #782 P4-B 2차).
 *
 * Service 가 SharingRulePolicyEvaluator + Repository 합성을 정확한 sObjectName / entityPath 로 호출하는지 확인.
 */
@DisplayName("AdminAccountService — getAccessibleAccountsByPolicy")
class AdminAccountSharingPolicyTest {

    private val accountRepository: AccountRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()

    private val service = AdminAccountService(accountRepository, policyEvaluator)

    @Test
    @DisplayName("Service 가 evaluator.buildPredicate(sObjectName=Account) 호출 후 Repository 에 Predicate 전달")
    fun servicePassesPredicateToRepository() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, userId = 100L)
        val stubPredicate = mockk<Predicate>()
        val expectedAccounts = listOf<Account>(mockk(), mockk())

        every {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "Account",
                entityPath = any<EntityPathBase<*>>(),
            )
        } returns stubPredicate
        every { accountRepository.findAllAccessibleByPolicy(stubPredicate) } returns expectedAccounts

        val result = service.getAccessibleAccountsByPolicy(scope)

        assertThat(result).isEqualTo(expectedAccounts)
        verify(exactly = 1) {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "Account",
                entityPath = any<EntityPathBase<*>>(),
            )
        }
        verify(exactly = 1) { accountRepository.findAllAccessibleByPolicy(stubPredicate) }
    }
}
