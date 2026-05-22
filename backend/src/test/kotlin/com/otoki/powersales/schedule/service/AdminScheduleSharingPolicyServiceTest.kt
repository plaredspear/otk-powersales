package com.otoki.powersales.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.EntityPathBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AdminScheduleSharingPolicyService Service layer 통합 검증 (spec #782 P4-B 2차).
 *
 * DisplayWorkSchedule + MFEIS 각각의 evaluator.buildPredicate 호출 인자 정합 + Repository 결과 전달 확인.
 */
@DisplayName("AdminScheduleSharingPolicyService — DisplayWorkSchedule / MFEIS")
class AdminScheduleSharingPolicyServiceTest {

    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()

    private val service = AdminScheduleSharingPolicyService(
        displayWorkScheduleRepository,
        mfeisRepository,
        policyEvaluator,
    )

    @Test
    @DisplayName("DisplayWorkSchedule — buildPredicate(sObjectName=DisplayWorkScheduleMaster__c) 호출")
    fun displayWorkSchedule() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, userId = 100L)
        val stubPredicate = mockk<Predicate>()
        val expected = listOf<DisplayWorkSchedule>(mockk())

        every {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "DisplayWorkScheduleMaster__c",
                entityPath = any<EntityPathBase<*>>(),
            )
        } returns stubPredicate
        every { displayWorkScheduleRepository.findAllAccessibleByPolicy(stubPredicate) } returns expected

        val result = service.getAccessibleDisplayWorkSchedulesByPolicy(scope)

        assertThat(result).isEqualTo(expected)
        verify(exactly = 1) {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "DisplayWorkScheduleMaster__c",
                entityPath = any<EntityPathBase<*>>(),
            )
        }
    }

    @Test
    @DisplayName("MFEIS — buildPredicate(sObjectName=MonthlyFemaleEmployeeIntegrationSchedule__c) 호출")
    fun mfeis() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, userId = 200L)
        val stubPredicate = mockk<Predicate>()
        val expected = listOf<MonthlyFemaleEmployeeIntegrationSchedule>(mockk())

        every {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c",
                entityPath = any<EntityPathBase<*>>(),
            )
        } returns stubPredicate
        every { mfeisRepository.findAllAccessibleByPolicy(stubPredicate) } returns expected

        val result = service.getAccessibleMfeisByPolicy(scope)

        assertThat(result).isEqualTo(expected)
        verify(exactly = 1) {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c",
                entityPath = any<EntityPathBase<*>>(),
            )
        }
    }
}
