package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.external.sap.outbound.sender.AttendanceSapSender
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.AttendancePayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.AttendanceSapPayload
import com.otoki.powersales.domain.activity.schedule.sap.AttendanceSapPayloadRow
import com.otoki.powersales.domain.activity.schedule.service.AttendanceBatchService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DisplayName("AttendanceBatchService — daily SAP outbound 처리 (#588 P1-B / #692)")
class AttendanceBatchServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val sender: AttendanceSapSender = mockk()
    private val payloadFactory = AttendancePayloadFactory()

    private val pageSize = 100
    private lateinit var service: AttendanceBatchService

    @BeforeEach
    fun setUp() {
        service = AttendanceBatchService(
            repository = repository,
            payloadFactory = payloadFactory,
            sender = sender,
            pageSize = pageSize,
        )
    }

    @Test
    @DisplayName("페이지 100건씩 분할 송신 — 250건 → SAP 호출 3회 (100/100/50)")
    fun runDaily_pagesArePushedSequentially() {
        val today = LocalDate.of(2026, 5, 4)
        val yesterday = today.minus(1, ChronoUnit.DAYS)
        every { repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(0)) } returns rows(100, today)
        every { repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(pageSize)) } returns rows(100, today)
        every { repository.findRegularAttendancesForSapPaged(eq(today), eq(yesterday), eq(pageSize), eq(2 * pageSize)) } returns rows(50, today)
        val captured = mutableListOf<AttendanceSapPayload>()
        every { sender.sendPage(capture(captured)) } returns true

        service.runDaily(today)

        verify(exactly = 3) { sender.sendPage(any()) }
        assertThat(captured.map { it.request.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun runDaily_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findRegularAttendancesForSapPaged(any(), any(), any(), any()) } returns emptyList()

        service.runDaily(today)

        verify(exactly = 0) { sender.sendPage(any()) }
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행 — 두 번째가 실패해도 1, 3 페이지 송신")
    fun runDaily_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(0)) } returns rows(100, today)
        every { repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(pageSize)) } returns rows(100, today)
        every { repository.findRegularAttendancesForSapPaged(any(), any(), eq(pageSize), eq(2 * pageSize)) } returns rows(50, today)
        every { sender.sendPage(any()) } returnsMany listOf(true, false, true)

        service.runDaily(today)

        verify(exactly = 3) { sender.sendPage(any()) }
    }

    private fun rows(count: Int, today: LocalDate): List<AttendanceSapPayloadRow> =
        (1..count).map { idx ->
            AttendanceSapPayloadRow(
                scheduleId = idx.toLong(),
                workingDate = today,
                employeeCode = "EMP$idx",
                accountExternalKey = "ACC$idx",
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory2 = null,
                workingCategory3 = null,
                secondWorkType = null
            )
        }
}
