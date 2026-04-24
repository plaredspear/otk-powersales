package com.otoki.powersales.schedule.service

import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.AccountRepository
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.sap.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.sap.repository.OrganizationRepository
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

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
    @Mock private lateinit var monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository
    @Mock private lateinit var holidayMasterRepository: HolidayMasterRepository

    @InjectMocks private lateinit var service: AdminMonthlyIntegrationService

    private fun setupCommonMocks() {
        whenever(organizationRepository.expandCostCenterCodes(any()))
            .thenReturn(listOf("CC001"))
        whenever(employeeRepository.findByCostCenterCodeInAndStatus(any(), any()))
            .thenReturn(listOf(createEmployee(id = 1L, costCenterCode = "CC001")))
        whenever(displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(any(), any()))
            .thenReturn(emptyList())
        whenever(accountRepository.findByIdIn(any()))
            .thenReturn(emptyList())
        whenever(monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()))
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

    @Nested
    @DisplayName("refreshIntegration - 통합일정 자동 갱신")
    inner class RefreshIntegrationTests {

        private val employeeId = 1L
        private val accountId = 100
        private val yearMonth = YearMonth.of(2026, 4)

        @Test
        @DisplayName("생성 후 집계 생성 - 첫 고정 일정 생성 시 통합일정 레코드 생성")
        fun createIntegration_firstSchedule() {
            // Given
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = "고정")
            ))
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(null)
            whenever(teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            )).thenReturn(1)
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()))
                .thenAnswer { it.getArgument<MonthlyFemaleEmployeeIntegrationSchedule>(0) }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).save(argThat<MonthlyFemaleEmployeeIntegrationSchedule> {
                this.year == "2026" &&
                    this.month == "04" &&
                    this.workingDaysMonth?.compareTo(BigDecimal.ONE) == 0 &&
                    this.numberOfInputs == 1L &&
                    this.equivalentNumberOfWorkingDays?.compareTo(BigDecimal.ONE) == 0
            })
        }

        @Test
        @DisplayName("격고 환산 계산 - 격고 일정 1건 시 환산근무일수=0.5")
        fun refreshIntegration_alternateType() {
            // Given
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = "격고")
            ))
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(null)
            whenever(teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            )).thenReturn(1)
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()))
                .thenAnswer { it.getArgument<MonthlyFemaleEmployeeIntegrationSchedule>(0) }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).save(argThat<MonthlyFemaleEmployeeIntegrationSchedule> {
                this.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("0.5")) == 0
            })
        }

        @Test
        @DisplayName("순회 환산 계산 - 3개 거래처 순회 시 환산근무일수=1/3")
        fun refreshIntegration_patrolType() {
            // Given
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 10), workingCategory3 = "순회")
            ))
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(null)
            whenever(teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 10))
            )).thenReturn(3)
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()))
                .thenAnswer { it.getArgument<MonthlyFemaleEmployeeIntegrationSchedule>(0) }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).save(argThat<MonthlyFemaleEmployeeIntegrationSchedule> {
                this.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("0.3333")) == 0
            })
        }

        @Test
        @DisplayName("삭제 후 집계 삭제 - 마지막 일정 삭제 시 통합일정 레코드 삭제")
        fun deleteIntegration_noSchedulesRemaining() {
            // Given
            val existing = MonthlyFemaleEmployeeIntegrationSchedule(
                id = 10L,
                year = "2026",
                month = "04",
                employee = Employee(id = employeeId, employeeCode = "E001", name = "테스트"),
                account = Account(id = accountId)
            )
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(existing)

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).delete(existing)
            verify(monthlyIntegrationScheduleRepository, never()).save(any())
        }

        @Test
        @DisplayName("일정 0건 + 기존 레코드 없음 - 아무 작업도 하지 않음")
        fun noSchedules_noExisting() {
            // Given
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(null)

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository, never()).delete(any())
            verify(monthlyIntegrationScheduleRepository, never()).save(any())
        }

        @Test
        @DisplayName("환산인원 계산 - 환산근무일수/영업일수")
        fun convertedHeadcount_calculation() {
            // Given: 4월 영업일수 = 22일(평일) - 0일(공휴일) = 22일, 고정 10일 근무
            val schedules = (1..10).map { day ->
                createScheduleRecord(
                    id = day.toLong(), employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, day), workingCategory3 = "고정"
                )
            }
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(schedules)
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(null)
            schedules.forEach { s ->
                whenever(teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                    eq(employeeId), eq(s.workingDate!!)
                )).thenReturn(1)
            }
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()))
                .thenAnswer { it.getArgument<MonthlyFemaleEmployeeIntegrationSchedule>(0) }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).save(argThat<MonthlyFemaleEmployeeIntegrationSchedule> {
                this.workingDaysMonth?.compareTo(BigDecimal("10")) == 0 &&
                    this.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("10")) == 0 &&
                    this.convertedHeadcount?.compareTo(BigDecimal("0.4545")) == 0
            })
        }

        @Test
        @DisplayName("기존 레코�� 업데이트 - 기존 통합일정 있으면 삭제 후 신규 생성")
        fun updateExisting() {
            // Given
            val existing = MonthlyFemaleEmployeeIntegrationSchedule(
                id = 10L, year = "2026", month = "04",
                employee = Employee(id = employeeId, employeeCode = "E001", name = "테스트"),
                account = Account(id = accountId)
            )
            whenever(teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            )).thenReturn(listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = "고정")
            ))
            whenever(monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            )).thenReturn(existing)
            whenever(teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            )).thenReturn(1)
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())
            whenever(monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()))
                .thenAnswer { it.getArgument<MonthlyFemaleEmployeeIntegrationSchedule>(0) }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(monthlyIntegrationScheduleRepository).delete(existing)
            verify(monthlyIntegrationScheduleRepository).save(any())
        }
    }

    @Nested
    @DisplayName("calculateBusinessDays - 영업일수 계산")
    inner class CalculateBusinessDaysTests {

        @Test
        @DisplayName("공휴일 없는 월 - 평일 수만 반환")
        fun noHolidays() {
            // Given: 2026년 4월 = 평일 22일
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(emptyList())

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(22)
        }

        @Test
        @DisplayName("공휴일 있는 월 - 평일 공휴일 제외")
        fun withHolidays() {
            // Given: 2026년 4월, 4/1(수) 공휴일
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(listOf(
                HolidayMaster(
                    holidayDate = LocalDate.of(2026, 4, 1),
                    name = "테스트공휴일",
                    type = "법정공휴일",
                    year = 2026
                )
            ))

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(21)
        }

        @Test
        @DisplayName("주말 공휴일 - 이미 주말이라 영업일 감소 없음")
        fun weekendHoliday() {
            // Given: 2026년 4월, 4/4(토) 공휴일
            whenever(holidayMasterRepository.findByHolidayDateBetween(any(), any())).thenReturn(listOf(
                HolidayMaster(
                    holidayDate = LocalDate.of(2026, 4, 4),
                    name = "토요일공휴일",
                    type = "법정공휴일",
                    year = 2026
                )
            ))

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(22)
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
            employee = Employee(id = employeeId, employeeCode = "EMP$employeeId", name = "테스트$employeeId"),
            account = Account(id = accountId),
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
