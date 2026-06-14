package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterPayloadFactory
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterSapPayload
import com.otoki.powersales.external.sap.outbound.sender.PPTMasterSapSender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DisplayName("PPTMasterSapBatchService — hourly SAP outbound 처리 (#765)")
class PPTMasterSapBatchServiceTest {

    private val repository: PPTMasterRepository = mockk()
    private val sender: PPTMasterSapSender = mockk()
    private val payloadFactory = PPTMasterPayloadFactory()

    private val pageSize = 100
    private lateinit var service: PPTMasterSapBatchService

    @BeforeEach
    fun setUp() {
        service = PPTMasterSapBatchService(
            pptMasterRepository = repository,
            payloadFactory = payloadFactory,
            sender = sender,
            pageSize = pageSize,
        )
    }

    @Test
    @DisplayName("페이지 100건씩 분할 송신 — 250건 → SAP 호출 3회 (100/100/50)")
    fun runHourly_pagesArePushedSequentially() {
        val today = LocalDate.of(2026, 5, 18)
        val monthFirstDay = today.withDayOfMonth(1)
        val monthLastDay = monthFirstDay.plusMonths(1).minus(1, ChronoUnit.DAYS)
        every { repository.findSapOutboundTargets(monthFirstDay, monthLastDay) } returns masters(250)
        val payloads = mutableListOf<PPTMasterSapPayload>()
        every { sender.sendPage(capture(payloads)) } returns true

        service.runHourly(today)

        verify(exactly = 3) { sender.sendPage(any()) }
        assertThat(payloads.map { it.REQUEST.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun runHourly_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 18)
        every { repository.findSapOutboundTargets(any(), any()) } returns emptyList()

        service.runHourly(today)

        verify(exactly = 0) { sender.sendPage(any()) }
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행 — 두 번째가 실패해도 1, 3 페이지 송신")
    fun runHourly_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 18)
        every { repository.findSapOutboundTargets(any(), any()) } returns masters(250)
        every { sender.sendPage(any()) } returnsMany listOf(true, false, true)

        service.runHourly(today)

        verify(exactly = 3) { sender.sendPage(any()) }
    }

    @Test
    @DisplayName("조회 범위 — monthFirstDay = today 의 1일, monthLastDay = 다음달 1일 - 1일")
    fun runHourly_monthBoundsForJanuary() {
        val today = LocalDate.of(2026, 1, 15)
        every { repository.findSapOutboundTargets(any(), any()) } returns emptyList()

        service.runHourly(today)

        verify {
            repository.findSapOutboundTargets(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
            )
        }
    }

    @Test
    @DisplayName("조회 범위 — 2월 경계 처리 (28/29일)")
    fun runHourly_monthBoundsForFebruary() {
        val today = LocalDate.of(2026, 2, 10)
        every { repository.findSapOutboundTargets(any(), any()) } returns emptyList()

        service.runHourly(today)

        verify {
            repository.findSapOutboundTargets(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28)
            )
        }
    }

    private fun masters(count: Int): List<ProfessionalPromotionTeamMaster> {
        val today = LocalDate.of(2026, 5, 18)
        return (1..count).map { idx ->
            ProfessionalPromotionTeamMaster(
                id = idx.toLong(),
                name = "PM%07d".format(idx),
                teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                accountSfid = "001A$idx",
                isConfirmed = true,
                startDate = today.minus(1, ChronoUnit.DAYS),
                endDate = today.plus(1, ChronoUnit.DAYS),
                branchCode = "BR$idx"
            )
        }
    }
}
