package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.promotion.service.PromotionSchedulesUpsertHelper
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminPromotionScheduleService.regenerateSchedules 테스트 (Spec #694)")
class AdminPromotionScheduleRegenerateTest {

    private val promotionRepository: PromotionRepository = mockk(relaxUnitFun = true)
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk(relaxUnitFun = true)
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper = mockk(relaxUnitFun = true)
    private val promotionSchedulesUpsertHelper: PromotionSchedulesUpsertHelper = mockk()

    private val service = AdminPromotionScheduleService(
        promotionRepository = promotionRepository,
        promotionEmployeeRepository = promotionEmployeeRepository,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        accountRepository = accountRepository,
        teamScheduleValidator = TeamScheduleValidator(teamMemberScheduleRepository, displayWorkScheduleRepository),
        teamMemberScheduleCascadeHelper = teamMemberScheduleCascadeHelper,
        promotionSchedulesUpsertHelper = promotionSchedulesUpsertHelper
    )

    @Test
    @DisplayName("regenerateSchedules -> helper.upsert(promotionId) 1회 위임 호출")
    fun regenerateSchedules_delegatesToHelper() {
        val expected = PromotionConfirmResponse(
            promotionId = 10L,
            totalEmployees = 3,
            upsertedTeamMemberSchedules = 3
        )
        every { promotionSchedulesUpsertHelper.upsert(10L) } returns expected

        val result = service.regenerateSchedules(10L)

        assertThat(result).isSameAs(expected)
        verify(exactly = 1) { promotionSchedulesUpsertHelper.upsert(10L) }
    }

    @Test
    @DisplayName("regenerateSchedules 재호출 멱등 -> helper.upsert 가 매번 같은 promotionId 로 호출")
    fun regenerateSchedules_idempotent_reinvocation() {
        val response1 = PromotionConfirmResponse(promotionId = 10L, totalEmployees = 3, upsertedTeamMemberSchedules = 3)
        val response2 = PromotionConfirmResponse(promotionId = 10L, totalEmployees = 3, upsertedTeamMemberSchedules = 3)
        every { promotionSchedulesUpsertHelper.upsert(10L) } returnsMany listOf(response1, response2)

        val r1 = service.regenerateSchedules(10L)
        val r2 = service.regenerateSchedules(10L)

        assertThat(r1.upsertedTeamMemberSchedules).isEqualTo(r2.upsertedTeamMemberSchedules)
        verify(exactly = 2) { promotionSchedulesUpsertHelper.upsert(10L) }
    }
}
