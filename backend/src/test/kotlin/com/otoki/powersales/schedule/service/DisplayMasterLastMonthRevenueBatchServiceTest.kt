package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.service.internal.LastMonthRevenueLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("DisplayMasterLastMonthRevenueBatchService — daily lastMonthRevenue 갱신 (#690)")
class DisplayMasterLastMonthRevenueBatchServiceTest {

    private val repository: DisplayWorkScheduleRepository = mockk()
    private val lookup: LastMonthRevenueLookup = mockk()

    private val pageSize = 100
    private lateinit var service: DisplayMasterLastMonthRevenueBatchService

    @BeforeEach
    fun setUp() {
        every { repository.updateLastMonthRevenueById(any(), any()) } returns 1L
        service = DisplayMasterLastMonthRevenueBatchService(
            repository = repository,
            lastMonthRevenueLookup = lookup,
            pageSize = pageSize,
        )
    }

    @Test
    @DisplayName("페이지 100건씩 분할 처리 — 250건 → 3 페이지 (100/100/50)")
    fun runDaily_pagesAreProcessedSequentially() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findValidForLastMonthRevenuePaged(eq(today), eq(pageSize), eq(0)) } returns entities(1..100)
        every { repository.findValidForLastMonthRevenuePaged(eq(today), eq(pageSize), eq(pageSize)) } returns entities(101..200)
        every { repository.findValidForLastMonthRevenuePaged(eq(today), eq(pageSize), eq(2 * pageSize)) } returns entities(201..250)
        every { lookup.forAccounts(any(), eq(today)) } returns emptyMap()

        service.runDaily(today)

        verify(exactly = 3) { lookup.forAccounts(any(), eq(today)) }
    }

    @Test
    @DisplayName("빈 결과 — lookup / update 호출 없음")
    fun runDaily_emptyResultSkipsEverything() {
        val today = LocalDate.of(2026, 5, 4)
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), any()) } returns emptyList()

        service.runDaily(today)

        verify(exactly = 0) { lookup.forAccounts(any(), any()) }
        verify(exactly = 0) { repository.updateLastMonthRevenueById(any(), any()) }
    }

    @Test
    @DisplayName("first-run 가드 — lastMonthRevenue == null 인 row 는 newRevenue==0 이어도 update")
    fun runDaily_firstRunGuardUpdatesNullEvenWhenNewRevenueIsZero() {
        val today = LocalDate.of(2026, 5, 4)
        val account = Account(id = 10, externalKey = "ACC010")
        val entity = entity(id = 1L, account = account, lastMonthRevenue = null)
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(entity)
        // lookup 반환 Map 에 account.id 없음 → newRevenue = BigDecimal.ZERO 로 분기
        every { lookup.forAccounts(any(), eq(today)) } returns emptyMap()

        service.runDaily(today)

        verify(exactly = 1) { repository.updateLastMonthRevenueById(eq(1L), eq(BigDecimal.ZERO)) }
    }

    @Test
    @DisplayName("동일 값 skip — lastMonthRevenue == newRevenue 이면 update 호출 안 함")
    fun runDaily_skipsRowWhenRevenueUnchanged() {
        val today = LocalDate.of(2026, 5, 4)
        val account = Account(id = 10, externalKey = "ACC010")
        val entity = entity(id = 1L, account = account, lastMonthRevenue = BigDecimal("5000000"))
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(entity)
        every { lookup.forAccounts(any(), eq(today)) } returns mapOf(10 to BigDecimal("5000000"))

        service.runDaily(today)

        verify(exactly = 0) { repository.updateLastMonthRevenueById(any(), any()) }
    }

    @Test
    @DisplayName("다른 값 update — lastMonthRevenue != newRevenue 이면 update")
    fun runDaily_updatesRowWhenRevenueChanged() {
        val today = LocalDate.of(2026, 5, 4)
        val account = Account(id = 10, externalKey = "ACC010")
        val entity = entity(id = 1L, account = account, lastMonthRevenue = BigDecimal("3000000"))
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(entity)
        every { lookup.forAccounts(any(), eq(today)) } returns mapOf(10 to BigDecimal("5000000"))

        service.runDaily(today)

        verify(exactly = 1) { repository.updateLastMonthRevenueById(eq(1L), eq(BigDecimal("5000000"))) }
    }

    @Test
    @DisplayName("BigDecimal compareTo 정합 — scale 만 다른 동일 값은 skip (예: 1000 vs 1000.0)")
    fun runDaily_compareToTreatsScaleDifferenceAsEqual() {
        val today = LocalDate.of(2026, 5, 4)
        val account = Account(id = 10, externalKey = "ACC010")
        val entity = entity(id = 1L, account = account, lastMonthRevenue = BigDecimal("1000"))
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(entity)
        every { lookup.forAccounts(any(), eq(today)) } returns mapOf(10 to BigDecimal("1000.00"))

        service.runDaily(today)

        verify(exactly = 0) { repository.updateLastMonthRevenueById(any(), any()) }
    }

    @Test
    @DisplayName("account == null row — newRevenue=ZERO 분기 (lastMonthRevenue null 이면 update, 0 이면 skip)")
    fun runDaily_handlesNullAccountRow() {
        val today = LocalDate.of(2026, 5, 4)
        val nullAccountNull = entity(id = 1L, account = null, lastMonthRevenue = null)
        val nullAccountZero = entity(id = 2L, account = null, lastMonthRevenue = BigDecimal.ZERO)
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(nullAccountNull, nullAccountZero)
        every { lookup.forAccounts(any(), eq(today)) } returns emptyMap()

        service.runDaily(today)

        // id=1 (null) → update with ZERO
        verify(exactly = 1) { repository.updateLastMonthRevenueById(eq(1L), eq(BigDecimal.ZERO)) }
        // id=2 (ZERO == newRevenue ZERO) → skip
        verify(exactly = 0) { repository.updateLastMonthRevenueById(eq(2L), any()) }
    }

    @Test
    @DisplayName("update 실패 시 다음 row 진행 — exception 전파 안 함")
    fun runDaily_updateFailureDoesNotStopBatch() {
        val today = LocalDate.of(2026, 5, 4)
        val account = Account(id = 10, externalKey = "ACC010")
        val a = entity(id = 1L, account = account, lastMonthRevenue = null)
        val b = entity(id = 2L, account = account, lastMonthRevenue = null)
        every { repository.findValidForLastMonthRevenuePaged(any(), any(), eq(0)) } returns listOf(a, b)
        every { lookup.forAccounts(any(), eq(today)) } returns mapOf(10 to BigDecimal("100"))
        every { repository.updateLastMonthRevenueById(eq(1L), any()) } throws RuntimeException("DB down")
        every { repository.updateLastMonthRevenueById(eq(2L), any()) } returns 1L

        service.runDaily(today)

        verify(exactly = 1) { repository.updateLastMonthRevenueById(eq(1L), any()) }
        verify(exactly = 1) { repository.updateLastMonthRevenueById(eq(2L), any()) }
    }

    private fun entities(range: IntRange): List<DisplayWorkSchedule> =
        range.map { idx ->
            entity(
                id = idx.toLong(),
                account = Account(id = idx, externalKey = "ACC$idx"),
                lastMonthRevenue = null,
            )
        }

    private fun entity(id: Long, account: Account?, lastMonthRevenue: BigDecimal?): DisplayWorkSchedule =
        DisplayWorkSchedule(
            id = id,
            typeOfWork1 = TypeOfWork1.DISPLAY,
            typeOfWork3 = TypeOfWork3.FIXED,
            typeOfWork5 = TypeOfWork5.REGULAR,
            employee = Employee(id = id, employeeCode = "EMP$id", name = "사원$id"),
            account = account,
            lastMonthRevenue = lastMonthRevenue,
        )
}
