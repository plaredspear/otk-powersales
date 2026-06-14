package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleCascadeHelper
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.EntityPathBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AdminPromotionEmployeeService — getAccessiblePromotionEmployeesByPolicy (spec #782 P4-B 2차).
 *
 * ControlledByParent — buildPredicate 호출의 sObjectName 이 **자식이 아닌 부모** (`DKRetail__Promotion__c`).
 * Service 가 자식 Repository (`findAllAccessibleByParentPolicy`) 에 부모 정책 Predicate 전달.
 */
@DisplayName("AdminPromotionEmployeeService — ControlledByParent")
class AdminPromotionEmployeeSharingPolicyTest {

    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk()
    private val promotionRepository: PromotionRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper =
        mockk(relaxUnitFun = true)

    private val service = AdminPromotionEmployeeService(
        promotionEmployeeRepository = promotionEmployeeRepository,
        promotionRepository = promotionRepository,
        employeeRepository = employeeRepository,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        policyEvaluator = policyEvaluator,
        teamMemberScheduleCascadeHelper = teamMemberScheduleCascadeHelper,
    )

    @Test
    @DisplayName("ControlledByParent — buildPredicate(sObjectName=Promotion 부모 SObject) 호출 + Repository.findAllAccessibleByParentPolicy 위임")
    fun controlledByParent() {
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = false, userId = 100L)
        val parentPredicate = mockk<Predicate>()
        val expected = listOf<PromotionEmployee>(mockk(), mockk())

        every {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "DKRetail__Promotion__c", // **부모** SObject (자식 PromotionEmployee 아님)
                entityPath = any<EntityPathBase<*>>(),
            )
        } returns parentPredicate
        every {
            promotionEmployeeRepository.findAllAccessibleByParentPolicy(parentPredicate)
        } returns expected

        val result = service.getAccessiblePromotionEmployeesByPolicy(scope)

        assertThat(result).isEqualTo(expected)
        verify(exactly = 1) {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "DKRetail__Promotion__c",
                entityPath = any<EntityPathBase<*>>(),
            )
        }
        verify(exactly = 1) { promotionEmployeeRepository.findAllAccessibleByParentPolicy(parentPredicate) }
    }
}
