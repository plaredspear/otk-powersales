package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.schedule.sap.DisplayMasterSapPayload
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

@DisplayName("DisplayMasterBatchService — daily SAP outbound 처리 (#588 P2-B / #692)")
class DisplayMasterBatchServiceTest {

    private val repository: DisplayWorkScheduleRepository = mock()
    private val sender: DisplayMasterSapSender = mock()
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
        whenever(repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(0))).thenReturn(entities(100))
        whenever(repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(pageSize))).thenReturn(entities(100))
        whenever(repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(2 * pageSize))).thenReturn(entities(50))
        whenever(sender.sendPage(any())).thenReturn(true)

        service.runDaily(today)

        val captor = argumentCaptor<DisplayMasterSapPayload>()
        verify(sender, times(3)).sendPage(captor.capture())
        assertThat(captor.allValues.map { it.request.size }).containsExactly(100, 100, 50)
    }

    @Test
    @DisplayName("빈 결과 — sender 호출 없음")
    fun runDaily_emptyResultSkipsSender() {
        val today = LocalDate.of(2026, 5, 4)
        whenever(repository.findValidForDisplayMasterSapPaged(any(), any(), any())).thenReturn(emptyList())

        service.runDaily(today)

        verify(sender, never()).sendPage(any())
    }

    @Test
    @DisplayName("페이지 실패 시 다음 페이지 진행")
    fun runDaily_failedPageDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 4)
        whenever(repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(0))).thenReturn(entities(100))
        whenever(repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(pageSize))).thenReturn(entities(100))
        whenever(repository.findValidForDisplayMasterSapPaged(any(), eq(pageSize), eq(2 * pageSize))).thenReturn(entities(50))
        whenever(sender.sendPage(any())).thenReturn(true, false, true)

        service.runDaily(today)

        verify(sender, times(3)).sendPage(any())
    }

    @Test
    @DisplayName("엔티티 → SAP payload row 매핑")
    fun runDaily_mapsEntityFieldsToPayload() {
        val today = LocalDate.of(2026, 5, 4)
        val entity = DisplayWorkSchedule(
            id = 7L,
            typeOfWork1 = "진열",
            typeOfWork3 = "정상",
            typeOfWork5 = "전문판촉팀",
            employee = Employee(id = 1L, employeeCode = "EMP777", name = "홍길동"),
            account = Account(id = 1, externalKey = "ACC777")
        )
        whenever(repository.findValidForDisplayMasterSapPaged(eq(today), eq(pageSize), eq(0))).thenReturn(listOf(entity))
        whenever(sender.sendPage(any())).thenReturn(true)

        service.runDaily(today)

        val captor = argumentCaptor<DisplayMasterSapPayload>()
        verify(sender, times(1)).sendPage(captor.capture())
        val item = captor.firstValue.request.single()
        assertThat(item.EmployeeCode).isEqualTo("EMP777")
        assertThat(item.SAPAccountCode).isEqualTo("ACC777")
        assertThat(item.WorkingCategory1).isEqualTo("진열")
        assertThat(item.WorkingCategory3).isEqualTo("정상")
        assertThat(item.WorkingCategory5).isEqualTo("전문판촉팀")
    }

    private fun entities(count: Int): List<DisplayWorkSchedule> =
        (1..count).map { idx ->
            DisplayWorkSchedule(
                id = idx.toLong(),
                typeOfWork1 = "진열",
                typeOfWork3 = "정상",
                typeOfWork5 = "전문판촉팀",
                employee = Employee(id = idx.toLong(), employeeCode = "EMP$idx", name = "사원$idx"),
                account = Account(id = idx, externalKey = "ACC$idx")
            )
        }
}
