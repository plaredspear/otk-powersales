package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.schedule.dto.response.MonthlyIntegrationScheduleItem
import com.otoki.powersales.schedule.dto.response.Suitability
import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.repository.EmployeeInputCriteriaMasterRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminSalesComparisonService 테스트")
class AdminSalesComparisonServiceTest {

    @Mock private lateinit var adminMonthlyIntegrationService: AdminMonthlyIntegrationService
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository
    @Mock private lateinit var accountCategoryMasterRepository: com.otoki.powersales.account.repository.AccountCategoryMasterRepository

    @InjectMocks private lateinit var service: AdminSalesComparisonService

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun account(id: Int, code: String, name: String, type: AccountType?): Account {
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
        workingCategory5: String?,
        convertedHeadcount: BigDecimal,
        avgClosingAmount: Long,
        totalInputCount: Int = 1,
        equivalentWorkingDays: BigDecimal = BigDecimal.ONE
    ): MonthlyIntegrationScheduleItem = MonthlyIntegrationScheduleItem(
        branchName = "지점A",
        accountBranchName = "지점A",
        accountCode = accountCode,
        accountName = accountName,
        employeeCode = employeeCode,
        title = null,
        employeeName = employeeName,
        workingCategory1 = workingCategory1,
        workingCategory3 = workingCategory3,
        workingCategory4 = null,
        workingCategory5 = workingCategory5,
        totalInputCount = totalInputCount,
        equivalentWorkingDays = equivalentWorkingDays,
        convertedHeadcount = convertedHeadcount,
        avgClosingAmount = avgClosingAmount
    )

    private fun criteria(
        category: TypeOfWork1 = TypeOfWork1.DISPLAY,
        fixed: BigDecimal,
        bifurcation: BigDecimal,
        boundary: BigDecimal,
        accountCategoryCode: String
    ): EmployeeInputCriteriaMaster {
        val cm = com.otoki.powersales.account.entity.AccountCategoryMaster(
            accountCode = accountCategoryCode,
            name = accountCategoryCode
        )
        return EmployeeInputCriteriaMaster(
            typeOfWork1 = category,
            fixed1PersonStandardAmount = fixed,
            bifurcationHalfPersonStandard = bifurcation,
            boundary = boundary,
            confirmed = true,
            isDeleted = false,
            category = cm
        )
    }

    @BeforeEach
    fun setup() {
        whenever(employeeInputCriteriaMasterRepository.findByTypeOfWork1AndConfirmedTrueAndIsDeletedNot(any(), any()))
            .thenReturn(emptyList())
    }

    @Nested
    @DisplayName("권한 범위 필터링 (applyScope)")
    inner class ApplyScopeTest {

        @Test
        fun `isAllBranches=true 면 사용자 입력 그대로 통과`() {
            val result = service.applyScope(allScope, listOf("CC001", "CC002"))
            assertThat(result).containsExactly("CC001", "CC002")
        }

        @Test
        fun `branchCodes 교집합으로 필터링`() {
            val scope = branchScope("CC001", "CC003")
            val result = service.applyScope(scope, listOf("CC001", "CC002"))
            assertThat(result).containsExactly("CC001")
        }

        @Test
        fun `권한 범위 밖 코드만 입력하면 AdminForbiddenException`() {
            val scope = branchScope("CC001")
            assertThatThrownBy {
                service.applyScope(scope, listOf("CC002", "CC003"))
            }.isInstanceOf(AdminForbiddenException::class.java)
        }

        @Test
        fun `branchCodes 비어있는 사용자가 코드 입력하면 AdminForbiddenException`() {
            val scope = branchScope()
            assertThatThrownBy {
                service.applyScope(scope, listOf("CC001"))
            }.isInstanceOf(AdminForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("거래처유형 picklist (getSearchCategories)")
    inner class GetSearchCategoriesTest {

        @Test
        fun `useSearch=true 항목을 accountCode 정렬로 반환`() {
            val cm1 = com.otoki.powersales.account.entity.AccountCategoryMaster(accountCode = "01", name = "대형마트")
            val cm2 = com.otoki.powersales.account.entity.AccountCategoryMaster(accountCode = "02", name = "체인")
            whenever(accountCategoryMasterRepository.findByUseSearchTrueAndIsDeletedNotOrderByAccountCode(eq(true)))
                .thenReturn(listOf(cm1, cm2))

            val result = service.getSearchCategories()

            assertThat(result).hasSize(2)
            assertThat(result[0].accountCode).isEqualTo("01")
            assertThat(result[0].name).isEqualTo("대형마트")
            assertThat(result[1].accountCode).isEqualTo("02")
            assertThat(result[1].name).isEqualTo("체인")
        }

        @Test
        fun `useSearch=true 항목 없으면 빈 리스트`() {
            whenever(accountCategoryMasterRepository.findByUseSearchTrueAndIsDeletedNotOrderByAccountCode(eq(true)))
                .thenReturn(emptyList())

            val result = service.getSearchCategories()

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("판정 규칙 (judgeSuitability)")
    inner class JudgeSuitabilityTest {

        @Test
        fun `순회는 무조건 적합`() {
            val result = service.judgeSuitability(
                workingCategory3 = "순회",
                avgClosingAmount = 0L,
                totalDisplayConverted = BigDecimal.ZERO,
                fixedStandard = null,
                fixedMin = null,
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEqualTo(Suitability.FIT.displayName)
        }

        @Test
        fun `환산인원 0이면 공백`() {
            val result = service.judgeSuitability(
                workingCategory3 = "고정",
                avgClosingAmount = 1_000_000L,
                totalDisplayConverted = BigDecimal.ZERO,
                fixedStandard = BigDecimal(500_000),
                fixedMin = BigDecimal(400_000),
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEmpty()
        }

        @Test
        fun `고정 - 기준금액 이상이면 적합`() {
            val result = service.judgeSuitability(
                workingCategory3 = "고정",
                avgClosingAmount = 1_500_000L,
                totalDisplayConverted = BigDecimal.ONE,
                fixedStandard = BigDecimal(1_000_000),
                fixedMin = BigDecimal(800_000),
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEqualTo(Suitability.FIT.displayName)
        }

        @Test
        fun `고정 - 최소금액 이상 기준금액 미만이면 경계`() {
            val result = service.judgeSuitability(
                workingCategory3 = "고정",
                avgClosingAmount = 900_000L,
                totalDisplayConverted = BigDecimal.ONE,
                fixedStandard = BigDecimal(1_000_000),
                fixedMin = BigDecimal(800_000),
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEqualTo(Suitability.BOUNDARY.displayName)
        }

        @Test
        fun `고정 - 최소금액 미만이면 재검토`() {
            val result = service.judgeSuitability(
                workingCategory3 = "고정",
                avgClosingAmount = 500_000L,
                totalDisplayConverted = BigDecimal.ONE,
                fixedStandard = BigDecimal(1_000_000),
                fixedMin = BigDecimal(800_000),
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEqualTo(Suitability.REVIEW.displayName)
        }

        @Test
        fun `격고는 격고 기준금액으로 비교`() {
            val result = service.judgeSuitability(
                workingCategory3 = "격고",
                avgClosingAmount = 700_000L,
                totalDisplayConverted = BigDecimal.ONE,
                fixedStandard = BigDecimal(1_000_000),
                fixedMin = BigDecimal(800_000),
                bifurcationStandard = BigDecimal(600_000),
                bifurcationMin = BigDecimal(500_000)
            )
            assertThat(result).isEqualTo(Suitability.FIT.displayName)
        }

        @Test
        fun `투입기준 부재면 공백`() {
            val result = service.judgeSuitability(
                workingCategory3 = "고정",
                avgClosingAmount = 1_000_000L,
                totalDisplayConverted = BigDecimal.ONE,
                fixedStandard = null,
                fixedMin = null,
                bifurcationStandard = null,
                bifurcationMin = null
            )
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("입력 검증")
    inner class ValidateTest {

        @Test
        fun `costCenterCodes 비어있으면 예외`() {
            assertThatThrownBy {
                service.getSummary(allScope, 2026, 5, emptyList())
            }.isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        fun `year 범위 밖이면 예외`() {
            assertThatThrownBy {
                service.getSummary(allScope, 1999, 5, listOf("CC001"))
            }.isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        fun `month 범위 밖이면 예외`() {
            assertThatThrownBy {
                service.getSummary(allScope, 2026, 13, listOf("CC001"))
            }.isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("집계 모드 응답 빌드")
    inner class SummaryTest {

        @Test
        fun `통합일정 결과가 비어있으면 빈 응답`() {
            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(emptyList())

            val response = service.getSummary(allScope, 2026, 5, listOf("CC001"))

            assertThat(response.year).isEqualTo(2026)
            assertThat(response.month).isEqualTo(5)
            assertThat(response.rows).hasSize(3)
            assertThat(response.rows.map { it.suitability })
                .containsExactly("적합", "경계", "재검토")
            assertThat(response.total.totalCount).isEqualTo(0)
        }

        @Test
        fun `진열 상시 사원 1명 + 적합 거래처 1건`() {
            val itm = item(
                accountCode = "A001",
                accountName = "거래처A",
                employeeCode = "E001",
                employeeName = "사원A",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                workingCategory5 = "상시",
                convertedHeadcount = BigDecimal.ONE,
                avgClosingAmount = 1_500_000L
            )
            val acc = account(1, "A001", "거래처A", AccountType.DISCOUNT_STORE)
            val cm = com.otoki.powersales.account.entity.AccountCategoryMaster(
                accountCode = AccountType.DISCOUNT_STORE.displayName,
                name = AccountType.DISCOUNT_STORE.displayName
            )
            val crit = EmployeeInputCriteriaMaster(
                typeOfWork1 = TypeOfWork1.DISPLAY,
                fixed1PersonStandardAmount = BigDecimal(1_000_000),
                bifurcationHalfPersonStandard = BigDecimal(600_000),
                boundary = BigDecimal("0.2"),
                confirmed = true,
                isDeleted = false,
                category = cm
            )

            whenever(adminMonthlyIntegrationService.buildIntegrationItems(eq(2026), eq(5), eq(listOf("CC001"))))
                .thenReturn(listOf(itm))
            whenever(accountRepository.findByExternalKeyIn(listOf("A001"))).thenReturn(listOf(acc))
            whenever(employeeInputCriteriaMasterRepository.findByTypeOfWork1AndConfirmedTrueAndIsDeletedNot(any(), any()))
                .thenReturn(listOf(crit))

            val response = service.getSummary(allScope, 2026, 5, listOf("CC001"))

            val fitRow = response.rows.first { it.suitability == "적합" }
            assertThat(fitRow.totalCount).isEqualTo(1)
            assertThat(fitRow.countsByCategory["대형마트"]).isEqualTo(1)
            assertThat(fitRow.accountIdsByCategory["대형마트"]).containsExactly(1)
            assertThat(response.total.totalCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("중간집계 모드")
    inner class MiddleTest {

        @Test
        fun `accountIds 필터링 적용`() {
            val itmA = item("A001", "거래처A", "E001", "사원A", "진열", "고정", "상시", BigDecimal.ONE, 1_500_000L)
            val itmB = item("B001", "거래처B", "E002", "사원B", "진열", "고정", "상시", BigDecimal.ONE, 800_000L)
            val accA = account(1, "A001", "거래처A", AccountType.DISCOUNT_STORE)
            val accB = account(2, "B001", "거래처B", AccountType.DISCOUNT_STORE)

            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(listOf(itmA, itmB))
            whenever(accountRepository.findByExternalKeyIn(any())).thenReturn(listOf(accA, accB))

            val response = service.getMiddle(allScope, 2026, 5, listOf("CC001"), listOf(1))

            assertThat(response.items).hasSize(1)
            assertThat(response.items.first().accountCode).isEqualTo("A001")
        }
    }

    @Nested
    @DisplayName("상세 모드")
    inner class DetailTest {

        @Test
        fun `근무형태 필터 적용 - 진열만 선택 시 행사 제외`() {
            val displayItm = item("A001", "거래처A", "E001", "사원A", "진열", "고정", "상시", BigDecimal.ONE, 1_500_000L)
            val eventItm = item("A001", "거래처A", "E002", "사원B", "행사", null, null, BigDecimal.ONE, 1_500_000L)
            val acc = account(1, "A001", "거래처A", AccountType.DISCOUNT_STORE)

            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(listOf(displayItm, eventItm))
            whenever(accountRepository.findByExternalKeyIn(any())).thenReturn(listOf(acc))

            val response = service.getDetail(allScope, 2026, 5, listOf("CC001"), emptyList(), "진열", null)

            assertThat(response.items).hasSize(1)
            assertThat(response.items.first().workingCategory1).isEqualTo("진열")
        }

        @Test
        fun `accountIds 비어있고 필터 없으면 모든 행 반환`() {
            val displayItm = item("A001", "거래처A", "E001", "사원A", "진열", "고정", "상시", BigDecimal.ONE, 1_500_000L)
            val eventItm = item("A001", "거래처A", "E002", "사원B", "행사", null, null, BigDecimal.ONE, 1_500_000L)
            val acc = account(1, "A001", "거래처A", AccountType.DISCOUNT_STORE)

            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(listOf(displayItm, eventItm))
            whenever(accountRepository.findByExternalKeyIn(any())).thenReturn(listOf(acc))

            val response = service.getDetail(allScope, 2026, 5, listOf("CC001"), emptyList(), null, null)

            assertThat(response.items).hasSize(2)
            assertThat(response.total.rowCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Excel export")
    inner class ExportTest {

        @Test
        fun `집계 export 결과는 xlsx 파일명 + 비어있지 않은 바이트`() {
            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(emptyList())

            val result = service.exportSummary(allScope, 2026, 5, listOf("CC001"))

            assertThat(result.filename).endsWith(".xlsx")
            assertThat(result.filename).contains("집계")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        fun `중간집계 export 결과는 xlsx 파일명 + 비어있지 않은 바이트`() {
            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(emptyList())

            val result = service.exportMiddle(allScope, 2026, 5, listOf("CC001"), emptyList())

            assertThat(result.filename).endsWith(".xlsx")
            assertThat(result.filename).contains("중간집계")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        fun `상세 export 결과는 xlsx 파일명 + 비어있지 않은 바이트`() {
            whenever(adminMonthlyIntegrationService.buildIntegrationItems(any(), any(), any()))
                .thenReturn(emptyList())

            val result = service.exportDetail(allScope, 2026, 5, listOf("CC001"), emptyList(), null, null)

            assertThat(result.filename).endsWith(".xlsx")
            assertThat(result.filename).contains("상세")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
