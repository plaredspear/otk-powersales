package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.entity.StaffReview
import com.otoki.powersales.domain.org.employee.repository.StaffReviewRepository
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.sales.service.MonthlySalesRow
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("StaffEvaluationService - 여사원 평가조회 (레거시 evaluationList 동등)")
class StaffEvaluationServiceTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val staffReviewRepository: StaffReviewRepository = mockk()

    private val service = StaffEvaluationService(
        teamMemberScheduleRepository,
        monthlySalesHistoryGateway,
        salesProgressRateMasterRepository,
        accountRepository,
        staffReviewRepository,
    )

    private val employeeId = 7L

    private fun account(id: Long, code: String, name: String, type: String) =
        Account(id = id, externalKey = code, name = name, accountType = type)

    private fun salesRow(accountId: Long, closing: Long) = MonthlySalesRow(
        sapAccountCode = "",
        salesDate = "202605",
        closingAmountSum = BigDecimal.valueOf(closing),
        accountId = accountId,
        abcClosingAmount1 = null,
    )

    private fun target(accountId: Long, month: String, rt: Double, fr: Double, rm: Double, fo: Double) =
        SalesProgressRateMaster(
            account = Account(id = accountId),
            targetYear = "2026",
            targetMonth = month,
            rtTargetAmount = rt,
            frTargetAmount = fr,
            rmTargetAmount = rm,
            foTargetAmount = fo,
        )

    @Test
    @DisplayName("담당 거래처별 목표/실적/달성률 + 지점평가 점수를 조회한다")
    fun returnsAccountRowsAndBranchScore() {
        every {
            teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(
                employeeId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1),
            )
        } returns listOf(1L, 2L)

        // account 2 는 월매출실적 row 가 없어 INNER JOIN 정합으로 제외되어야 함
        every {
            monthlySalesHistoryGateway.findBySalesDatesByAccountId(listOf("202605"), listOf(1L, 2L))
        } returns listOf(salesRow(accountId = 1L, closing = 8_000_000L))

        every { accountRepository.findByIdIn(listOf(1L)) } returns
            listOf(account(1L, "A001", "행복마트", "슈퍼"))

        every {
            salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026")
        } returns listOf(target(1L, "5", rt = 5_000_000.0, fr = 3_000_000.0, rm = 2_000_000.0, fo = 0.0))

        every {
            staffReviewRepository.findByEmployeeIdAndFirstDayOfMonth(employeeId, LocalDate.of(2026, 5, 1))
        } returns listOf(StaffReview(employeeTotalScore = 27.0))

        val result = service.getEvaluation(employeeId, "202605")

        assertThat(result.yearMonth).isEqualTo("202605")
        assertThat(result.branchScore).isEqualTo(27.0)
        assertThat(result.branchMaxScore).isEqualTo(30)
        assertThat(result.accounts).hasSize(1)
        val row = result.accounts.first()
        assertThat(row.accountCode).isEqualTo("A001")
        assertThat(row.accountName).isEqualTo("행복마트")
        assertThat(row.accountType).isEqualTo("슈퍼")
        assertThat(row.targetAmount).isEqualTo(10_000_000L)
        assertThat(row.performanceAmount).isEqualTo(8_000_000L)
        assertThat(row.attainmentRate).isEqualTo(80.0) // round(8,000,000 / 10,000,000 * 100)
    }

    @Test
    @DisplayName("목표 미등록 거래처는 달성률 0 으로 산출한다")
    fun zeroAttainmentWhenNoTarget() {
        every {
            teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any())
        } returns listOf(1L)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns
            listOf(salesRow(accountId = 1L, closing = 5_000_000L))
        every { accountRepository.findByIdIn(listOf(1L)) } returns
            listOf(account(1L, "A001", "행복마트", "슈퍼"))
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()
        every { staffReviewRepository.findByEmployeeIdAndFirstDayOfMonth(any(), any()) } returns emptyList()

        val result = service.getEvaluation(employeeId, "202605")

        val row = result.accounts.single()
        assertThat(row.targetAmount).isEqualTo(0L)
        assertThat(row.performanceAmount).isEqualTo(5_000_000L)
        assertThat(row.attainmentRate).isEqualTo(0.0)
        assertThat(result.branchScore).isNull()
    }

    @Test
    @DisplayName("담당 거래처가 없으면 거래처 목록은 비고 지점평가만 반환한다")
    fun emptyAccountsButBranchKept() {
        every {
            teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any())
        } returns emptyList()
        every {
            staffReviewRepository.findByEmployeeIdAndFirstDayOfMonth(employeeId, LocalDate.of(2026, 5, 1))
        } returns listOf(StaffReview(employeeTotalScore = 25.5))

        val result = service.getEvaluation(employeeId, "202605")

        assertThat(result.accounts).isEmpty()
        assertThat(result.branchScore).isEqualTo(25.5)
    }

    @Test
    @DisplayName("조회월 미지정 시 전월을 기본 조회월로 사용한다")
    fun defaultsToPreviousMonth() {
        every {
            teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any())
        } returns emptyList()
        every { staffReviewRepository.findByEmployeeIdAndFirstDayOfMonth(any(), any()) } returns emptyList()

        val result = service.getEvaluation(employeeId, null)

        val expected = LocalDate.now().minusMonths(1)
        assertThat(result.yearMonth).isEqualTo("%04d%02d".format(expected.year, expected.monthValue))
    }
}
