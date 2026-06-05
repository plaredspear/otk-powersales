package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.schedule.sap.DisplayMasterSapPayload
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("DisplayMasterBatchService — daily SAP outbound 처리 (#588 P2-B / #692)")
class DisplayMasterBatchServiceTest {

    private val repository: DisplayWorkScheduleRepository = mockk()
    private val sender: DisplayMasterSapSender = mockk()
    private val payloadFactory = DisplayMasterPayloadFactory()

    private val pageSize = 100
    private lateinit var service: DisplayMasterBatchService

    @BeforeEach
    fun setUp() {
        service = DisplayMasterBatchService(
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
        every { repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(0)) } returns entities(100)
        every { repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(pageSize)) } returns entities(100)
        every { repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(2 * pageSize)) } returns entities(50)
        val captured = mutableListOf<DisplayMasterSapPayload>()
        every { sender.sendPage(capture(captured)) } returns true

        service.runDaily(today)

        verify(exactly = 3) { sender.sendPage(any()) }
        assertThat(captured.map { it.request.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun runDaily_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findValidForDisplayMasterSapPaged(any(), any(), any()) } returns emptyList()

        service.runDaily(today)

        verify(exactly = 0) { sender.sendPage(any()) }
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행")
    fun runDaily_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(0)) } returns entities(100)
        every { repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(pageSize)) } returns entities(100)
        every { repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(2 * pageSize)) } returns entities(50)
        every { sender.sendPage(any()) } returnsMany listOf(true, false, true)

        service.runDaily(today)

        verify(exactly = 3) { sender.sendPage(any()) }
    }

    @Test
    @DisplayName("엔티티 → SAP payload row 매핑")
    fun runDaily_mapsEntityFieldsToPayload() {
        val today = LocalDate.of(2026, 5, 4)
        val entity = DisplayWorkSchedule(
            id = 7L,
            typeOfWork1 = TypeOfWork1.DISPLAY,
            typeOfWork3 = TypeOfWork3.FIXED,
            typeOfWork5 = TypeOfWork5.REGULAR,
            employee = Employee(id = 1L, employeeCode = "EMP777", name = "홍길동"),
            account = Account(id = 1, externalKey = "ACC777")
        )
        every { repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(0)) } returns listOf(entity)
        val captured = mutableListOf<DisplayMasterSapPayload>()
        every { sender.sendPage(capture(captured)) } returns true

        service.runDaily(today)

        verify(exactly = 1) { sender.sendPage(any()) }
        val item = captured.first().request.single()
        assertThat(item.EmployeeCode).isEqualTo("EMP777")
        assertThat(item.SAPAccountCode).isEqualTo("ACC777")
        assertThat(item.WorkingCategory1).isEqualTo("진열")
        assertThat(item.WorkingCategory3).isEqualTo("고정")
        assertThat(item.WorkingCategory5).isEqualTo("상시")
    }

    private fun entities(count: Int): List<DisplayWorkSchedule> =
        (1..count).map { idx ->
            DisplayWorkSchedule(
                id = idx.toLong(),
                typeOfWork1 = TypeOfWork1.DISPLAY,
                typeOfWork3 = TypeOfWork3.FIXED,
                typeOfWork5 = TypeOfWork5.REGULAR,
                employee = Employee(id = idx.toLong(), employeeCode = "EMP$idx", name = "사원$idx"),
                account = Account(id = idx.toLong(), externalKey = "ACC$idx")
            )
        }
}
