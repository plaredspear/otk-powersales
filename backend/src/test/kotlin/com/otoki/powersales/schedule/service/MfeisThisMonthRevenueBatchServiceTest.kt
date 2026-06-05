package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.sales.service.MonthlySalesRow
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

@DisplayName("MfeisThisMonthRevenueBatchService — RDS 기반 양수 평균 산출 회귀 보호")
class MfeisThisMonthRevenueBatchServiceTest {

    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk(relaxed = true)
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()

    private lateinit var service: MfeisThisMonthRevenueBatchService

    @BeforeEach
    fun setUp() {
        service = MfeisThisMonthRevenueBatchService(
            mfeisRepository = mfeisRepository,
            monthlySalesHistoryGateway = monthlySalesHistoryGateway,
            chunkSize = 200,
        )
    }

    private fun account(id: Long, externalKey: String?): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
    }

    private fun mfeisRow(id: Long, account: Account?, currentAmount: BigDecimal?) =
        mockk<MonthlyFemaleEmployeeIntegrationSchedule>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.account } returns account
            every { this@mockk.thisMonthAmount } returns currentAmount
        }

    private fun row(sapCode: String, abc1: Long) =
        MonthlySalesRow(
            sapAccountCode = sapCode,
            salesDate = "",
            closingAmountSum = BigDecimal(abc1),
            abcClosingAmount1 = BigDecimal(abc1),
        )

    @Test
    @DisplayName("양수 필터 — 음수/0 매출 월은 합산/divider 모두 제외 후 평균 계산")
    fun positiveAmountFilter() {
        val targetYm = YearMonth.of(2026, 4)
        val account = account(1, "S001")
        val mfeis = mfeisRow(100L, account, BigDecimal.ZERO)

        every {
            mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing("2026", "04", "%상시%")
        } returns listOf(mfeis)
        every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns listOf(
            row("S001", 100_000),
            row("S001", 200_000),
            row("S001", 0),         // 제외
            row("S001", -50_000),   // 제외
            row("S001", 300_000),
            row("S001", 400_000),
        )
        val saved = slot<MonthlyFemaleEmployeeIntegrationSchedule>()
        every { mfeisRepository.save(capture(saved)) } answers { saved.captured }

        service.runMonthly(targetYm)

        // (100_000 + 200_000 + 300_000 + 400_000) / 4 = 250_000
        verify { mfeis.thisMonthAmount = BigDecimal("250000") }
    }

    @Test
    @DisplayName("externalKey null Account 만 → skip + 게이트웨이 호출 안 함")
    fun nullExternalKeySkipsGatewayCall() {
        val targetYm = YearMonth.of(2026, 4)
        val mfeis = mfeisRow(100L, account(1, externalKey = null), BigDecimal.ZERO)
        every {
            mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing("2026", "04", "%상시%")
        } returns listOf(mfeis)

        service.runMonthly(targetYm)

        verify(exactly = 0) { monthlySalesHistoryGateway.findBySalesDates(any(), any()) }
        verify(exactly = 0) { mfeisRepository.save(any()) }
    }

    @Test
    @DisplayName("동일 값 → save 호출 안 함 (legacy 동등)")
    fun sameAmountSkipsSave() {
        val targetYm = YearMonth.of(2026, 4)
        val account = account(1, "S001")
        val mfeis = mfeisRow(100L, account, BigDecimal("100000"))
        every {
            mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing("2026", "04", "%상시%")
        } returns listOf(mfeis)
        every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns listOf(
            row("S001", 100_000),
        )

        service.runMonthly(targetYm)

        verify(exactly = 0) { mfeisRepository.save(any()) }
    }
}
