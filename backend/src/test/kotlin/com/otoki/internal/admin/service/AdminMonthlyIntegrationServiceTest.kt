package com.otoki.internal.admin.service

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.sap.repository.OrganizationRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminMonthlyIntegrationService 테스트")
class AdminMonthlyIntegrationServiceTest {

    @Mock private lateinit var organizationRepository: OrganizationRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @Mock private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @InjectMocks private lateinit var service: AdminMonthlyIntegrationService

    private fun setupCommonMocks() {
        whenever(organizationRepository.expandCostCenterCodes(any()))
            .thenReturn(listOf("CC001"))
        whenever(employeeRepository.findByCostCenterCodeInAndStatus(any(), any()))
            .thenReturn(listOf(createEmployee(id = 1L, costCenterCode = "CC001")))
        whenever(displayWorkScheduleRepository.findByEmployeeIdInAndAccountIdIn(any(), any()))
            .thenReturn(emptyList())
        whenever(accountRepository.findByIdIn(any()))
            .thenReturn(emptyList())
        whenever(monthlySalesHistoryRepository.findByAccountExternalKeyInAndSalesYearIn(any(), any()))
            .thenReturn(emptyList())
        whenever(organizationRepository.searchForAdmin(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(emptyList())
    }

    @Nested
    @DisplayName("getMonthlyIntegration - 통합일정 조회")
    inner class GetMonthlyIntegrationTests {

        @Test
        @DisplayName("정상 조회 - 유효한 파라미터 -> 통합일정 결과 반환")
        fun success() {
            // Given
            setupCommonMocks()
            whenever(teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()))
                .thenReturn(listOf(
                    createScheduleRecord(id = 1L, employeeId = 1L, accountId = 100, workingDate = LocalDate.of(2026, 3, 1)),
                    createScheduleRecord(id = 2L, employeeId = 1L, accountId = 100, workingDate = LocalDate.of(2026, 3, 2))
                ))
            whenever(accountRepository.findByIdIn(any()))
                .thenReturn(listOf(createAccount(id = 100, externalKey = "A001", name = "이마트 강남점")))

            // When
            val result = service.getMonthlyIntegration(2026, 3, listOf("CC001"))

            // Then
            assertThat(result.year).isEqualTo(2026)
            assertThat(result.month).isEqualTo(3)
            assertThat(result.items).hasSize(1)
            assertThat(result.totalCount).isEqualTo(1)

            val item = result.items[0]
            assertThat(item.accountCode).isEqualTo("A001")
            assertThat(item.totalInputCount).isEqualTo(2)
            assertThat(item.equivalentWorkingDays).isEqualByComparingTo(BigDecimal("2.000"))
            assertThat(item.convertedHeadcount).isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        @DisplayName("데이터 없는 월 조회 - 빈 결과 반환")
        fun emptyResult() {
            // Given
            setupCommonMocks()
            whenever(teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()))
                .thenReturn(emptyList())

            // When
            val result = service.getMonthlyIntegration(2020, 1, listOf("CC001"))

            // Then
            assertThat(result.items).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("환산근무일수 검증 - 사원A가 3/1에 거래처 2곳 투입 시 각 0.5")
        fun equivalentWorkingDays_twoAccounts() {
            // Given
            val date = LocalDate.of(2026, 3, 1)
            setupCommonMocks()
            whenever(teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()))
                .thenReturn(listOf(
                    createScheduleRecord(id = 1L, employeeId = 1L, accountId = 100, workingDate = date),
                    createScheduleRecord(id = 2L, employeeId = 1L, accountId = 200, workingDate = date)
                ))
            whenever(accountRepository.findByIdIn(any()))
                .thenReturn(listOf(
                    createAccount(id = 100, externalKey = "A001"),
                    createAccount(id = 200, externalKey = "A002")
                ))

            // When
            val result = service.getMonthlyIntegration(2026, 3, listOf("CC001"))

            // Then
            assertThat(result.items).hasSize(2)
            result.items.forEach { item ->
                assertThat(item.equivalentWorkingDays).isEqualByComparingTo(BigDecimal("0.500"))
            }
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class ValidationTests {

        @Test
        @DisplayName("year 범위 초과 -> InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy {
                service.getMonthlyIntegration(1999, 3, listOf("CC001"))
            }.isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("month 범위 초과 -> InvalidParameterException")
        fun invalidMonth() {
            assertThatThrownBy {
                service.getMonthlyIntegration(2026, 13, listOf("CC001"))
            }.isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("cost_center_codes 빈 목록 -> InvalidParameterException")
        fun emptyCostCenterCodes() {
            assertThatThrownBy {
                service.getMonthlyIntegration(2026, 3, emptyList())
            }.isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getCategorySchedule - 근무형태별 인원현황")
    inner class GetCategoryScheduleTests {

        @Test
        @DisplayName("정상 조회 - 카테고리별 집계 결과 반환")
        fun success() {
            // Given
            setupCommonMocks()
            whenever(teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()))
                .thenReturn(
                    listOf(createScheduleRecord(
                        id = 1L, employeeId = 1L, accountId = 100,
                        workingDate = LocalDate.of(2026, 3, 1),
                        workingCategory1 = "진열", workingCategory3 = "고정"
                    ))
                )
            whenever(accountRepository.findByIdIn(any()))
                .thenReturn(listOf(createAccount(id = 100, externalKey = "A001")))

            // When
            val result = service.getCategorySchedule(2026, 3, listOf("CC001"))

            // Then
            assertThat(result.year).isEqualTo(2026)
            assertThat(result.month).isEqualTo(3)
        }
    }

    // --- Helpers ---

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "200001",
        name: String = "김영희",
        costCenterCode: String = "CC001"
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            name = name,
            costCenterCode = costCenterCode,
            status = "재직"
        )
    }

    private fun createScheduleRecord(
        id: Long = 1L,
        employeeId: Long = 1L,
        accountId: Int = 100,
        workingDate: LocalDate = LocalDate.of(2026, 3, 1),
        workingCategory1: String = "진열",
        workingCategory3: String? = "고정",
        workingCategory4: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            employeeId = employeeId,
            accountId = accountId,
            workingDate = workingDate,
            workingType = "근무",
            workingCategory1 = workingCategory1,
            workingCategory3 = workingCategory3,
            workingCategory4 = workingCategory4,
            commuteLogId = "CL001"
        )
    }

    private fun createAccount(
        id: Int = 100,
        externalKey: String = "A001",
        name: String = "이마트",
        branchName: String? = null
    ): Account {
        return Account(
            id = id,
            externalKey = externalKey,
            name = name,
            branchName = branchName
        )
    }
}
