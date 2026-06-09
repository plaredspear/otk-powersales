package com.otoki.powersales.sales.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.sales.repository.WorkingDayMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("MonthlySalesService 테스트")
class MonthlySalesServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository = mockk()
    private val workingDayMasterRepository: WorkingDayMasterRepository = mockk()
    private val service = MonthlySalesService(
        accountRepository,
        monthlySalesHistoryGateway,
        salesProgressRateMasterRepository,
        workingDayMasterRepository,
    )

    @Nested
    @DisplayName("getMonthlySales — RDS 기반 응답")
    inner class GetMonthlySalesTests {

        @Test
        @DisplayName("customerId / yearMonth 가 응답에 그대로 전달된다")
        fun returnsRequestEcho() {
            // "C001" 은 숫자가 아니라 SAP 코드 직접 호출 경로 — account 미매칭(목표 없음).
            every { accountRepository.findByExternalKey("C001") } returns null
            every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns emptyList()

            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "C001", yearMonth = "202602")
            )

            assertThat(result.customerId).isEqualTo("C001")
            assertThat(result.yearMonth).isEqualTo("202602")
            assertThat(result.achievedAmount).isEqualTo(0L)
            assertThat(result.targetAmount).isEqualTo(0L)
        }

        @Test
        @DisplayName("customerId 가 null 이면 ALL 로 응답 + RDS 호출 안 함")
        fun nullCustomerIdReturnsAll() {
            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = null, yearMonth = "202602")
            )

            assertThat(result.customerId).isEqualTo("ALL")
            assertThat(result.achievedAmount).isEqualTo(0L)
        }

        @Test
        @DisplayName("숫자 customerId(=account.id) → SAP 코드 resolve 후 실적/목표/달성률 산출")
        fun numericCustomerIdResolvesAccountAndTarget() {
            // account.id=100 → SAP 코드 1000091 resolve (물류매출 service 와 동일 규약).
            val account = mockk<Account> {
                every { id } returns 100L
                every { externalKey } returns "1000091"
                every { name } returns "(주)이마트 월배점"
            }
            every {
                accountRepository.findByIdInAndIsDeletedNot(listOf(100L), true)
            } returns listOf(account)

            // 조회월(202605) 마감 합계 실적 7,796만원. 카테고리 개별 컬럼은 비고 합계만 적재된 케이스.
            every { monthlySalesHistoryGateway.findBySalesDates(any(), listOf("1000091")) } returns listOf(
                MonthlySalesRow(
                    sapAccountCode = "1000091",
                    salesDate = "202605",
                    closingAmountSum = BigDecimal("77960000"),
                    abcClosingAmount1 = null,
                )
            )

            // 목표: 상온 4,597만 / 라면 2,040만 / 냉동냉장 2,519만 / 유지 41만 → 합계 9,197만.
            val target = mockk<SalesProgressRateMaster> {
                every { isDeleted } returns null
                every { targetMonth } returns "5"
                every { rtTargetAmount } returns 45_970_000.0
                every { rmTargetAmount } returns 20_400_000.0
                every { frTargetAmount } returns 25_190_000.0
                every { foTargetAmount } returns 410_000.0
            }
            every {
                salesProgressRateMasterRepository.findByAccountIdAndTargetYear(100L, "2026")
            } returns listOf(target)

            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "100", yearMonth = "202605")
            )

            assertThat(result.customerName).isEqualTo("(주)이마트 월배점")
            assertThat(result.achievedAmount).isEqualTo(77_960_000L)
            assertThat(result.targetAmount).isEqualTo(91_970_000L)
            // round(7796 / 9197 * 100) = 85
            assertThat(result.achievementRate).isEqualTo(85.0)
            assertThat(result.categorySales).hasSize(4)
            val ambient = result.categorySales.first { it.category == "AMBIENT" }
            assertThat(ambient.targetAmount).isEqualTo(45_970_000L)
        }

        @Test
        @DisplayName("과거 월 조회 시 기준 진도율(baseRate) 은 0 (레거시 calcBusinessRateOnlyThisMonth 정합)")
        fun baseRateZeroForPastMonth() {
            every { accountRepository.findByExternalKey("C001") } returns null
            every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns emptyList()

            // 2000년 1월은 시스템 당월이 아니므로 baseRate=0.
            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "C001", yearMonth = "200001")
            )

            assertThat(result.baseRate).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("baseRate — WorkingDayMaster 기반 기준 진도율 (레거시 calcBusinessRateOnlyThisMonth)")
    inner class BaseRateTests {

        @Test
        @DisplayName("당월 조회 시 WorkingDayMaster 영업일 count 로 (경과/전체)×100 산출")
        fun currentMonthUsesWorkingDayMaster() {
            val now = LocalDate.now()
            val yearMonth = "%04d%02d".format(now.year, now.monthValue)
            val firstDay = LocalDate.of(now.year, now.monthValue, 1)
            val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())

            every { accountRepository.findByExternalKey("C001") } returns null
            every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns emptyList()
            // 전체 영업일 20, 월초~오늘 경과 영업일 10 → 50%.
            every { workingDayMasterRepository.countWorkingDays(firstDay, lastDay, 1) } returns 20L
            if (now != lastDay) {
                every { workingDayMasterRepository.countWorkingDays(firstDay, now, 1) } returns 10L
            }
            val expected = if (now == lastDay) 100.0 else 50.0

            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "C001", yearMonth = yearMonth)
            )

            assertThat(result.baseRate).isEqualTo(expected)
        }

        @Test
        @DisplayName("당월이라도 WorkingDayMaster 미적재(전체 영업일 0)면 0")
        fun currentMonthEmptyMasterReturnsZero() {
            val now = LocalDate.now()
            val yearMonth = "%04d%02d".format(now.year, now.monthValue)
            val firstDay = LocalDate.of(now.year, now.monthValue, 1)
            val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())

            every { accountRepository.findByExternalKey("C001") } returns null
            every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns emptyList()
            every { workingDayMasterRepository.countWorkingDays(firstDay, lastDay, 1) } returns 0L

            val result = service.getMonthlySales(
                MonthlySalesRequest(customerId = "C001", yearMonth = yearMonth)
            )

            assertThat(result.baseRate).isEqualTo(0.0)
        }
    }
}
