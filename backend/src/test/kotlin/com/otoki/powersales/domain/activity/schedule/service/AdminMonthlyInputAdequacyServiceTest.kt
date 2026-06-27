package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyInputAdequacyService
import com.otoki.powersales.domain.activity.schedule.service.AdminSalesComparisonService
import com.otoki.powersales.domain.activity.schedule.dto.response.AccountCategoryColumn
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.Suitability
import com.otoki.powersales.domain.activity.schedule.service.InvalidParameterException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import io.mockk.every
import io.mockk.mockk

@DisplayName("AdminMonthlyInputAdequacyService 테스트")
class AdminMonthlyInputAdequacyServiceTest {

    private val adminSalesComparisonService: AdminSalesComparisonService = mockk()

    private val service = AdminMonthlyInputAdequacyService(
        adminSalesComparisonService,
    )

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun account(id: Long, code: String, name: String, type: AccountType?): Account {
        val acc = Account(id = id, externalKey = code)
        acc.name = name
        acc.accountType = type
        return acc
    }

    private fun item(
        accountCode: String,
        accountName: String,
        employeeCode: String,
        employeeName: String,
        workingCategory1: String,
        workingCategory3: String?,
        workingCategory5: String?
    ): MonthlyIntegrationScheduleItem = MonthlyIntegrationScheduleItem(
        branchName = "지점A",
        accountBranchName = "지점A",
        accountCode = accountCode,
        accountName = accountName,
        distributionChannelLabel = null,
        abcTypeLabel = null,
        employeeCode = employeeCode,
        title = null,
        employeeName = employeeName,
        workingCategory1 = workingCategory1,
        workingCategory3 = workingCategory3,
        workingCategory4 = null,
        workingCategory5 = workingCategory5,
        totalInputCount = 1,
        equivalentWorkingDays = BigDecimal.ONE,
        convertedHeadcount = BigDecimal.ONE,
        avgClosingAmount = 0L
    )

    private fun suitability(
        account: Account,
        empItems: List<MonthlyIntegrationScheduleItem>
    ) = AdminSalesComparisonService.AccountSuitability(
        account = account,
        accountCode = account.externalKey ?: "",
        accountName = account.name ?: "",
        accountBranchName = "지점A",
        accountCategory = AccountCategoryColumn.HYPER.displayName,
        accountCategoryCode = "01",
        totalDisplayConvertedHeadcount = BigDecimal.ONE,
        totalEventConvertedHeadcount = BigDecimal.ZERO,
        totalDisplayHeadcount = 1,
        totalInputCount = 1,
        totalEquivalentWorkingDays = BigDecimal.ONE,
        avgClosingAmount = 1_000_000L,
        thisMonthSalesAmount = 1_000_000L,
        fixedStandardAmount = BigDecimal(800_000),
        fixedMinAmount = BigDecimal(400_000),
        bifurcationHalfStandardAmount = null,
        bifurcationHalfMinAmount = null,
        allEmployeeItems = empItems,
        ediPos = null
    )

    @Nested
    @DisplayName("입력 검증 (validateParams)")
    inner class ValidateParamsTest {

        @Test
        fun `year 범위 밖이면 InvalidParameterException`() {
            assertThatThrownBy {
                service.getMatrix(allScope, year = 1999, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)
            }.isInstanceOf(InvalidParameterException::class.java)

            assertThatThrownBy {
                service.getMatrix(allScope, year = 2100, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)
            }.isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        fun `costCenterCodes 비어있으면 InvalidParameterException`() {
            assertThatThrownBy {
                service.getMatrix(allScope, year = 2025, costCenterCodes = emptyList(), workingCategory3Filter = null)
            }.isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("권한 범위 필터링 (applyScope)")
    inner class ApplyScopeTest {

        @Test
        fun `권한 범위 밖 코드만 입력하면 AdminForbiddenException`() {
            val scope = branchScope("CC001")
            assertThatThrownBy {
                service.getMatrix(scope, year = 2025, costCenterCodes = listOf("CC999"), workingCategory3Filter = null)
            }.isInstanceOf(AdminForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("매트릭스 빌드")
    inner class BuildMatrixTest {

        @Test
        fun `진열 상시 사원 1건 + 1월만 적합 데이터 → 1월=적합 + 나머지=공백`() {
            val acc = account(1, "ACC001", "거래처A", AccountType.DISCOUNT_STORE)
            val empItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "홍길동",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                workingCategory5 = "상시"
            )
            // 1월만 데이터, 2~12월은 빈 리스트
            every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(1), any()) } returns listOf(suitability(acc, listOf(empItem)))
            (2..12).forEach { m ->
                every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(m), any()) } returns emptyList()
            }
            // 1월 fixed 비교 1_000_000 / 1 = 1_000_000 >= 800_000 → 적합
            every {
                adminSalesComparisonService.judgeSuitability(
                    workingCategory3 = eq("고정"),
                    avgClosingAmount = eq(1_000_000L),
                    totalDisplayConverted = eq(BigDecimal.ONE),
                    fixedStandard = eq(BigDecimal(800_000)),
                    fixedMin = eq(BigDecimal(400_000)),
                    bifurcationStandard = null,
                    bifurcationMin = null
                )
} returns Suitability.FIT.displayName
            val response = service.getMatrix(allScope, year = 2025, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)

            assertThat(response.year).isEqualTo(2025)
            assertThat(response.items).hasSize(1)
            val row = response.items.first()
            assertThat(row.employeeCode).isEqualTo("E001")
            assertThat(row.accountCode).isEqualTo("ACC001")
            assertThat(row.workingCategory3).isEqualTo("고정")
            assertThat(row.monthlySuitability).hasSize(12)
            assertThat(row.monthlySuitability[0]).isEqualTo(Suitability.FIT.displayName)
            (1..11).forEach { idx ->
                assertThat(row.monthlySuitability[idx]).isEmpty()
            }
        }

        @Test
        fun `진열 상시 아닌 사원은 매트릭스 행으로 채택되지 않음`() {
            val acc = account(1, "ACC001", "거래처A", AccountType.DISCOUNT_STORE)
            val eventItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E002",
                employeeName = "행사사원",
                workingCategory1 = "행사",
                workingCategory3 = "고정",
                workingCategory5 = "임시"
            )
            (1..12).forEach { m ->
                every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(m), any()) } returns listOf(suitability(acc, listOf(eventItem)))
            }

            val response = service.getMatrix(allScope, year = 2025, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)

            assertThat(response.items).isEmpty()
        }

        @Test
        fun `동일 사원·거래처 × 다른 workingCategory3 는 행 분리 (레거시 동등 — 행 키에 workingCategory3 포함)`() {
            val acc = account(1, "ACC001", "거래처A", AccountType.DISCOUNT_STORE)
            // 같은 사원 E001 이 1월=고정 / 2월=격고 로 동일 거래처에 투입 (운영 데이터 변동 케이스)
            val janFixedItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "홍길동",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                workingCategory5 = "상시"
            )
            val febBifurcationItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "홍길동",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                workingCategory5 = "상시"
            )
            every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(1), any()) } returns listOf(suitability(acc, listOf(janFixedItem)))
            every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(2), any()) } returns listOf(suitability(acc, listOf(febBifurcationItem)))
            (3..12).forEach { m ->
                every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(m), any()) } returns emptyList()
            }
            every {
                adminSalesComparisonService.judgeSuitability(
                    workingCategory3 = any(),
                    avgClosingAmount = any(),
                    totalDisplayConverted = any(),
                    fixedStandard = any(),
                    fixedMin = any(),
                    bifurcationStandard = any(),
                    bifurcationMin = any()
                )
} returns Suitability.FIT.displayName

            val response = service.getMatrix(allScope, year = 2025, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)

            // 행 2건 (고정 / 격고) 분리
            assertThat(response.items).hasSize(2)
            val workTypes = response.items.map { it.workingCategory3 }.toSet()
            assertThat(workTypes).containsExactlyInAnyOrder("고정", "격고")
            // 고정 행은 1월만 적합 / 격고 행은 2월만 적합
            val fixedRow = response.items.first { it.workingCategory3 == "고정" }
            val bifurcationRow = response.items.first { it.workingCategory3 == "격고" }
            assertThat(fixedRow.monthlySuitability[0]).isEqualTo(Suitability.FIT.displayName)
            assertThat(fixedRow.monthlySuitability[1]).isEmpty()
            assertThat(bifurcationRow.monthlySuitability[0]).isEmpty()
            assertThat(bifurcationRow.monthlySuitability[1]).isEqualTo(Suitability.FIT.displayName)
        }

        @Test
        fun `workingCategory3 필터 — 고정만 선택하면 격고 사원 제외`() {
            val acc = account(1, "ACC001", "거래처A", AccountType.DISCOUNT_STORE)
            val fixedItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "고정사원",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                workingCategory5 = "상시"
            )
            val bifurcationItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E002",
                employeeName = "격고사원",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                workingCategory5 = "상시"
            )
            every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(1), any()) } returns listOf(suitability(acc, listOf(fixedItem, bifurcationItem)))
            (2..12).forEach { m ->
                every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(m), any()) } returns emptyList()
            }
            every {
                adminSalesComparisonService.judgeSuitability(
                    workingCategory3 = any(),
                    avgClosingAmount = any(),
                    totalDisplayConverted = any(),
                    fixedStandard = any(),
                    fixedMin = any(),
                    bifurcationStandard = any(),
                    bifurcationMin = any()
                )
} returns Suitability.FIT.displayName
            val response = service.getMatrix(allScope, year = 2025, costCenterCodes = listOf("CC001"), workingCategory3Filter = "고정")

            assertThat(response.items).hasSize(1)
            assertThat(response.items.first().employeeCode).isEqualTo("E001")
            assertThat(response.items.first().workingCategory3).isEqualTo("고정")
        }
    }

    @Nested
    @DisplayName("엑셀 export (exportMatrix)")
    inner class ExportMatrixTest {

        @Test
        fun `매트릭스 export 결과는 xlsx 파일명 + 비어있지 않은 바이트`() {
            val acc = account(1, "ACC001", "거래처A", AccountType.DISCOUNT_STORE)
            val empItem = item(
                accountCode = "ACC001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "홍길동",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                workingCategory5 = "상시"
            )
            every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(1), any()) } returns listOf(suitability(acc, listOf(empItem)))
            (2..12).forEach { m ->
                every { adminSalesComparisonService.computeAccountSuitabilities(eq(2025), eq(m), any()) } returns emptyList()
            }
            every {
                adminSalesComparisonService.judgeSuitability(
                    workingCategory3 = any(),
                    avgClosingAmount = any(),
                    totalDisplayConverted = any(),
                    fixedStandard = any(),
                    fixedMin = any(),
                    bifurcationStandard = any(),
                    bifurcationMin = any()
                )
            } returns Suitability.FIT.displayName

            val result = service.exportMatrix(allScope, year = 2025, costCenterCodes = listOf("CC001"), workingCategory3Filter = null)

            assertThat(result.filename).endsWith(".xlsx")
            assertThat(result.filename).contains("월별투입적정성")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
