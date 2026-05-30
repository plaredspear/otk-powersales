package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("AdminConvertedHeadcountReportService 테스트 (Spec #847)")
class AdminConvertedHeadcountReportServiceTest {

    private val repository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk()
    private val service = AdminConvertedHeadcountReportService(repository)

    private fun account(type: AccountType, branchName: String?): Account {
        val acc = Account(id = type.ordinal + 1)
        acc.accountType = type
        acc.branchName = branchName
        return acc
    }

    private fun row(
        accountType: AccountType,
        accountBranchName: String? = "거래처지점",
        empBranchName: String? = "서울지점",
        workingCategory1: String? = "근무A",
        year: String? = "2026",
        month: String? = "5",
        convertedHeadcount: BigDecimal? = BigDecimal("1.0"),
    ): MonthlyFemaleEmployeeIntegrationSchedule =
        MonthlyFemaleEmployeeIntegrationSchedule(
            year = year,
            month = month,
            workingCategory1 = workingCategory1,
            empBranchName = empBranchName,
            convertedHeadcount = convertedHeadcount,
            account = account(accountType, accountBranchName),
        )

    /** repository.findConvertedHeadcountReport(any...) 를 주어진 결과로 stub. */
    private fun stubReport(result: List<MonthlyFemaleEmployeeIntegrationSchedule>) {
        every {
            repository.findConvertedHeadcountReport(
                year = any(),
                month = any(),
                workingCategory5In = any(),
                includeNullWc5 = any(),
                excludeConsignment = any(),
                costCenterCode = any(),
            )
        } returns result
    }

    @Nested
    @DisplayName("variant 필터 전달")
    inner class VariantFilter {

        @Test
        @DisplayName("variant 의 근무유형5/위탁제외/코스트센터 파라미터를 repository 에 그대로 전달한다")
        fun passesVariantFiltersToRepository() {
            val wc5Slot = slot<List<String>>()
            val includeNullSlot = slot<Boolean>()
            val excludeConsignSlot = slot<Boolean>()
            val costCenterSlot = slot<String?>()
            every {
                repository.findConvertedHeadcountReport(
                    year = any(),
                    month = any(),
                    workingCategory5In = capture(wc5Slot),
                    includeNullWc5 = capture(includeNullSlot),
                    excludeConsignment = capture(excludeConsignSlot),
                    costCenterCode = captureNullable(costCenterSlot),
                )
            } returns emptyList()

            service.getReport(ConvertedHeadcountReportVariant.TEAM2_PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(wc5Slot.captured).containsExactlyInAnyOrder("상시", "임시")
            assertThat(includeNullSlot.captured).isTrue()
            assertThat(excludeConsignSlot.captured).isFalse()
            assertThat(costCenterSlot.captured).isEqualTo("4889")
        }

        @Test
        @DisplayName("위탁농협 제외 variant 는 excludeConsignment=true 를 전달한다")
        fun passesExcludeConsignment() {
            val excludeConsignSlot = slot<Boolean>()
            every {
                repository.findConvertedHeadcountReport(
                    year = any(),
                    month = any(),
                    workingCategory5In = any(),
                    includeNullWc5 = any(),
                    excludeConsignment = capture(excludeConsignSlot),
                    costCenterCode = any(),
                )
            } returns emptyList()

            service.getReport(ConvertedHeadcountReportVariant.PERMANENT_ONLY_EXCL_CONSIGN, "2026", "5")

            assertThat(excludeConsignSlot.captured).isTrue()
        }
    }

    @Nested
    @DisplayName("집계/그룹/소계/합계")
    inner class Aggregation {

        @Test
        @DisplayName("동일 구분×근무유형1×지점×연월 환산인원을 합산한다")
        fun sumsSameKey() {
            stubReport(
                listOf(
                    row(AccountType.DISCOUNT_STORE, convertedHeadcount = BigDecimal("1.5")),
                    row(AccountType.DISCOUNT_STORE, convertedHeadcount = BigDecimal("0.5")),
                ),
            )

            val res = service.getReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.groups).hasSize(1)
            assertThat(res.groups[0].rows).hasSize(1)
            assertThat(res.groups[0].rows[0].convertedHeadcount).isEqualByComparingTo("2.0")
        }

        @Test
        @DisplayName("구분(거래처유형)별로 그룹을 나누고 소계를 계산한다")
        fun groupsByAccountTypeWithSubtotal() {
            stubReport(
                listOf(
                    row(AccountType.DISCOUNT_STORE, convertedHeadcount = BigDecimal("2.0")),
                    row(AccountType.SUPER, convertedHeadcount = BigDecimal("3.0")),
                ),
            )

            val res = service.getReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.groups).hasSize(2)
            val byType = res.groups.associateBy { it.accountType }
            assertThat(byType["할인점"]!!.subtotalHeadcount).isEqualByComparingTo("2.0")
            assertThat(byType["수퍼"]!!.subtotalHeadcount).isEqualByComparingTo("3.0")
        }

        @Test
        @DisplayName("전체 합계는 그룹 소계의 합이다")
        fun totalIsSumOfSubtotals() {
            stubReport(
                listOf(
                    row(AccountType.DISCOUNT_STORE, convertedHeadcount = BigDecimal("2.0")),
                    row(AccountType.SUPER, convertedHeadcount = BigDecimal("3.0")),
                ),
            )

            val res = service.getReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.totalHeadcount).isEqualByComparingTo("5.0")
        }

        @Test
        @DisplayName("연월은 month 를 2자리로 패딩하여 year-month 로 구성한다")
        fun composesYearMonth() {
            stubReport(listOf(row(AccountType.SUPER, year = "2026", month = "5")))

            val res = service.getReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.groups[0].rows[0].yearMonth).isEqualTo("2026-05")
        }
    }

    @Nested
    @DisplayName("variant 별 지점 기준")
    inner class BranchScope {

        @Test
        @DisplayName("1-x variant 는 여사원 소속(empBranchName) 을 지점으로 쓴다")
        fun usesEmpBranchForTeam1() {
            stubReport(
                listOf(row(AccountType.SUPER, empBranchName = "서울지점", accountBranchName = "거래처지점")),
            )

            val res = service.getReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.groups[0].rows[0].branchName).isEqualTo("서울지점")
        }

        @Test
        @DisplayName("2-1 variant 는 거래처 지점(account.branchName) 을 지점으로 쓴다")
        fun usesAccountBranchForTeam2() {
            stubReport(
                listOf(row(AccountType.SUPER, empBranchName = "서울지점", accountBranchName = "거래처지점")),
            )

            val res = service.getReport(ConvertedHeadcountReportVariant.TEAM2_PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(res.groups[0].rows[0].branchName).isEqualTo("거래처지점")
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("variant 한글명 + 연-월 파일명 xlsx 생성")
        fun exportsXlsx() {
            stubReport(listOf(row(AccountType.SUPER)))

            val result = service.exportReport(ConvertedHeadcountReportVariant.PERMANENT_TEMP_ALL, "2026", "5")

            assertThat(result.filename).isEqualTo("거래처유형별환산인원(상시임시전체)_2026-5.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        @DisplayName("결과 0건도 헤더만 있는 xlsx 를 생성한다")
        fun exportsEmptyXlsx() {
            stubReport(emptyList())

            val result = service.exportReport(ConvertedHeadcountReportVariant.TEMP_ALL, "2026", "5")

            assertThat(result.bytes).isNotEmpty()
        }
    }
}
