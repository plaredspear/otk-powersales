package com.otoki.powersales.schedule.sap

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.sap.outbound.sender.AttendanceSapSender
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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

@DisplayName("AttendanceDailyBatch — daily SAP outbound 배치 (#588 P1-B)")
class AttendanceDailyBatchTest {

    private val repository: TeamMemberScheduleRepository = mock()
    private val sender: AttendanceSapSender = mock()
    private val payloadFactory = AttendancePayloadFactory()
    private val runner: ScheduledJobRunner = mock()

    private val pageSize = 100
    private lateinit var batch: AttendanceDailyBatch

    @BeforeEach
    fun setUp() {
        batch = AttendanceDailyBatch(
            repository = repository,
            payloadFactory = payloadFactory,
            sender = sender,
            scheduledJobRunner = runner,
            pageSize = pageSize
        )
    }

    @Test
    @DisplayName("페이지 100건씩 분할 송신 — 250건 → SAP 호출 3회 (100/100/50)")
    fun execute_pagesArePushedSequentially() {
        val today = LocalDate.of(2026, 5, 4)
        val yesterday = today.minusDays(1)
        whenever(repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(0))).thenReturn(rows(100, today))
        whenever(repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(pageSize))).thenReturn(rows(100, today))
        whenever(repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(2 * pageSize))).thenReturn(rows(50, today))
        whenever(sender.sendPage(any())).thenReturn(true)

        batch.execute(today)

        val captor = argumentCaptor<AttendanceSapPayload>()
        verify(sender, times(3)).sendPage(captor.capture())
        assertThat(captor.allValues.map { it.request.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun execute_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 4)
        whenever(repository.findRegularAttendancesForSapPaged(any(), any(), any(), any())).thenReturn(emptyList())

        batch.execute(today)

        verify(sender, never()).sendPage(any())
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행 — 두 번째 페이지가 실패해도 1, 3 페이지는 전송 시도")
    fun execute_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 4)
        whenever(repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(0))).thenReturn(rows(100, today))
        whenever(repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(pageSize))).thenReturn(rows(100, today))
        whenever(repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(2 * pageSize))).thenReturn(rows(50, today))
        whenever(sender.sendPage(any())).thenReturn(true, false, true)

        batch.execute(today)

        verify(sender, times(3)).sendPage(any())
    }

    private fun rows(count: Int, today: LocalDate): List<AttendanceSapPayloadRow> =
        (1..count).map { idx ->
            AttendanceSapPayloadRow(
                attendanceLogId = idx.toLong(),
                workingDate = today,
                employeeCode = "EMP$idx",
                accountExternalKey = "ACC$idx",
                workingCategory1 = "근무",
                workingCategory2 = null,
                workingCategory3 = null,
                secondWorkType = null
            )
        }
}
