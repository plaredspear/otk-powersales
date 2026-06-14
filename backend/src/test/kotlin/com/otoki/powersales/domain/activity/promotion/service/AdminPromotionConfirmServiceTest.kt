package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionConfirmResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminPromotionConfirmService 테스트 (helper 위임)")
class AdminPromotionConfirmServiceTest {

    private val helper: PromotionSchedulesUpsertHelper = mockk()
    private val service = AdminPromotionConfirmService(helper)

    @Test
    @DisplayName("confirmPromotion -> helper.upsert(promotionId) 1회 위임 호출")
    fun confirmPromotion_delegatesToHelper() {
        val expected = PromotionConfirmResponse(
            promotionId = 10L,
            totalEmployees = 3,
            upsertedTeamMemberSchedules = 3
        )
        every { helper.upsert(10L) } returns expected

        val result = service.confirmPromotion(10L)

        assertThat(result).isSameAs(expected)
        verify(exactly = 1) { helper.upsert(10L) }
    }
}
