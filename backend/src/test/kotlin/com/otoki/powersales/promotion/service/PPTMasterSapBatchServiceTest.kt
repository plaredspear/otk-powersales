package com.otoki.powersales.promotion.service

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import com.otoki.powersales.promotion.sap.PPTMasterPayloadFactory
import com.otoki.powersales.promotion.sap.PPTMasterSapPayload
import com.otoki.powersales.sap.outbound.sender.PPTMasterSapSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@DisplayName("PPTMasterSapBatchService — hourly SAP outbound 처리 (#765)")
class PPTMasterSapBatchServiceTest {

    private val repository: PPTMasterRepository = mock()
    private val sender: PPTMasterSapSender = mock()
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
        val monthLastDay = monthFirstDay.plusMonths(1).minus(1, java.time.temporal.ChronoUnit.DAYS)
        whenever(repository.findSapOutboundTargets(eq(monthFirstDay), eq(monthLastDay))).thenReturn(masters(250))
        whenever(sender.sendPage(any())).thenReturn(true)

        service.runHourly(today)

        val captor = argumentCaptor<PPTMasterSapPayload>()
        verify(sender, times(3)).sendPage(captor.capture())
        assertThat(captor.allValues.map { it.REQUEST.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun runHourly_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 18)
        whenever(repository.findSapOutboundTargets(any(), any())).thenReturn(emptyList())

        service.runHourly(today)

        verify(sender, never()).sendPage(any())
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행 — 두 번째가 실패해도 1, 3 페이지 송신")
    fun runHourly_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 18)
        whenever(repository.findSapOutboundTargets(any(), any())).thenReturn(masters(250))
        whenever(sender.sendPage(any())).thenReturn(true, false, true)

        service.runHourly(today)

        verify(sender, times(3)).sendPage(any())
    }

    @Test
    @DisplayName("조회 범위 — monthFirstDay = today 의 1일, monthLastDay = 다음달 1일 - 1일")
    fun runHourly_monthBoundsForJanuary() {
        val today = LocalDate.of(2026, 1, 15)
        whenever(repository.findSapOutboundTargets(any(), any())).thenReturn(emptyList())

        service.runHourly(today)

        verify(repository).findSapOutboundTargets(
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 1, 31))
        )
    }

    @Test
    @DisplayName("조회 범위 — 2월 경계 처리 (28/29일)")
    fun runHourly_monthBoundsForFebruary() {
        val today = LocalDate.of(2026, 2, 10)
        whenever(repository.findSapOutboundTargets(any(), any())).thenReturn(emptyList())

        service.runHourly(today)

        verify(repository).findSapOutboundTargets(
            eq(LocalDate.of(2026, 2, 1)),
            eq(LocalDate.of(2026, 2, 28))
        )
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
                startDate = today.minus(1, java.time.temporal.ChronoUnit.DAYS),
                endDate = today.plus(1, java.time.temporal.ChronoUnit.DAYS),
                branchCode = "BR$idx"
            )
        }
    }
}
