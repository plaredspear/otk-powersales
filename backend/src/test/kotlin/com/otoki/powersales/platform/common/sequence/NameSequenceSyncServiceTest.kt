package com.otoki.powersales.platform.common.sequence

import com.otoki.powersales.domain.activity.inspection.repository.SiteActivityRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionProductRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("NameSequenceSyncService 테스트")
class NameSequenceSyncServiceTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk()
    private val promotionRepository: PromotionRepository = mockk()
    private val promotionProductRepository: PromotionProductRepository = mockk()
    private val pptMasterRepository: PPTMasterRepository = mockk()
    private val pptHistoryRepository: PPTHistoryRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val siteActivityRepository: SiteActivityRepository = mockk()
    private val transactionTemplate: TransactionTemplate = mockk()

    private val service = NameSequenceSyncService(
        teamMemberScheduleRepository,
        promotionEmployeeRepository,
        promotionRepository,
        promotionProductRepository,
        pptMasterRepository,
        pptHistoryRepository,
        appointmentRepository,
        siteActivityRepository,
        transactionTemplate,
    )

    init {
        // 시퀀스별 독립 트랜잭션 — 콜백을 그대로 실행해 예외가 service 로 전파되게 한다.
        every { transactionTemplate.execute<Long>(any()) } answers {
            firstArg<TransactionCallback<Long>>().doInTransaction(mockk(relaxed = true))
        }
        every { teamMemberScheduleRepository.syncNameSeq() } returns 1L
        every { promotionEmployeeRepository.syncNameSeq() } returns 2L
        every { promotionRepository.syncPromotionNumberSeq() } returns 3L
        every { promotionProductRepository.syncNameSeq() } returns 4L
        every { pptMasterRepository.syncNameSeq() } returns 5L
        every { pptHistoryRepository.syncNameSeq() } returns 6L
        every { appointmentRepository.syncNameSeq() } returns 7L
        every { siteActivityRepository.syncNameSeq() } returns 8L
    }

    @Test
    @DisplayName("채번 시퀀스 8종을 모두 보정하고 보정값을 반환한다")
    fun syncAll_syncsEverySequence() {
        val result = service.syncAll(reason = "test")

        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "team_member_schedule.name" to 1L,
                "promotion_employee.name" to 2L,
                "promotion.promotion_number" to 3L,
                "promotion_product.name" to 4L,
                "professional_promotion_team_master.name" to 5L,
                "professional_promotion_team_history.name" to 6L,
                "appointment.name" to 7L,
                "site_activity.name" to 8L,
            )
        )
    }

    @Test
    @DisplayName("한 시퀀스가 실패해도 나머지 보정은 계속된다 (실패 격리)")
    fun syncAll_isolatesFailure() {
        every { promotionRepository.syncPromotionNumberSeq() } throws RuntimeException("sequence not found")

        val result = service.syncAll(reason = "test")

        assertThat(result).hasSize(7)
        assertThat(result).doesNotContainKey("promotion.promotion_number")
        // 실패 이후 대상도 정상 호출되어야 한다.
        verify(exactly = 1) { siteActivityRepository.syncNameSeq() }
        verify(exactly = 1) { appointmentRepository.syncNameSeq() }
    }
}
