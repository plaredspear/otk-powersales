package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.enums.HolidayType
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.account.entity.AccountCategoryMaster
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

@DisplayName("AdminMonthlyIntegrationService 테스트")
class AdminMonthlyIntegrationServiceTest {

    private val organizationRepository: OrganizationRepository = mockk(relaxUnitFun = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk(relaxUnitFun = true)
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk(relaxUnitFun = true)
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk(relaxUnitFun = true)
    private val holidayMasterRepository: HolidayMasterRepository = mockk(relaxUnitFun = true)
    private val branchCodeExpander: BranchCodeExpander = mockk(relaxUnitFun = true)
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository = mockk(relaxUnitFun = true)
    private val employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository = mockk(relaxUnitFun = true)

    private val service = AdminMonthlyIntegrationService(
        organizationRepository,
        employeeRepository,
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
        accountRepository,
        monthlySalesHistoryRepository,
        monthlyIntegrationScheduleRepository,
        holidayMasterRepository,
        branchCodeExpander,
        accountCategoryMasterRepository,
        employeeInputCriteriaMasterRepository,
    )

    @BeforeEach
    fun setupRefreshIntegrationDefaults() {
        // spec #680 §5.3 — refreshIntegration 의 3필드 set 통합 + §5.2 buildIntegrationItems 의
        // Q12 옵션 1 (MFEIS persist 우선) 로 인한 신규 의존 호출 default stub.
        // 본 default 는 호출되어도 NPE/MockKException 안 나게만 보장. 개별 테스트가 필요시 override.
        every { displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(any(), any()) } returns emptyList()
        every {
            monthlyIntegrationScheduleRepository.findByAccountIdAndWorkingCategory1AndYearAndMonth(
                any(), any(), any(), any()
            )
        } returns emptyList()
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                any(), any(), any(), any()
            )
        } returns null
        // spec #680 §5.3 (v1.3) — EmployeeInputCriteriaMaster lookup 의존
        every { accountCategoryMasterRepository.findByName(any()) } returns null
        every {
            employeeInputCriteriaMasterRepository.findActiveByCategoryAndTypeOfWork1(
                any(), any(), any()
            )
        } returns null
    }

    private fun setupCommonMocks() {
        every { organizationRepository.expandCostCenterCodes(any()) } returns listOf("CC001")
        every { branchCodeExpander.expand(any()) } answers { (firstArg<Collection<String>>()).toSet() }
        every { employeeRepository.findByCostCenterCodeInAndStatus(any(), any()) } returns listOf(createEmployee(id = 1L, costCenterCode = "CC001"))
        every { displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(any(), any()) } returns emptyList()
        every { accountRepository.findByIdIn(any()) } returns emptyList()
        every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns emptyList()
        every { organizationRepository.searchForAdmin(any(), any(), any()) } returns emptyList()
    }

    @Nested
    @DisplayName("getMonthlyIntegration - 통합일정 조회")
    inner class GetMonthlyIntegrationTests {

        @Test
        @DisplayName("정상 조회 - 유효한 파라미터 -> 통합일정 결과 반환")
        fun success() {
            // Given
            setupCommonMocks()
            every { teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()) } returns listOf(
                    createScheduleRecord(id = 1L, employeeId = 1L, accountId = 100, workingDate = LocalDate.of(2026, 3, 1)),
                    createScheduleRecord(id = 2L, employeeId = 1L, accountId = 100, workingDate = LocalDate.of(2026, 3, 2))
                )
            every { accountRepository.findByIdIn(any()) } returns listOf(createAccount(id = 100, externalKey = "A001", name = "이마트 강남점"))

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
            every { teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()) } returns emptyList()

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
            every { teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()) } returns listOf(
                    createScheduleRecord(id = 1L, employeeId = 1L, accountId = 100, workingDate = date),
                    createScheduleRecord(id = 2L, employeeId = 1L, accountId = 200, workingDate = date)
                )
            every { accountRepository.findByIdIn(any()) } returns listOf(
                    createAccount(id = 100, externalKey = "A001"),
                    createAccount(id = 200, externalKey = "A002")
                )

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
    @DisplayName("getCategorySchedule - 근무형태별 여사원인원현황")
    inner class GetCategoryScheduleTests {

        @Test
        @DisplayName("정상 조회 - 카테고리별 집계 결과 반환")
        fun success() {
            // Given
            setupCommonMocks()
            every { teamMemberScheduleRepository.findIntegrationScheduleRecords(any(), any(), any()) } returns 
                    listOf(createScheduleRecord(
                        id = 1L, employeeId = 1L, accountId = 100,
                        workingDate = LocalDate.of(2026, 3, 1),
                        workingCategory1 = WorkingCategory1.DISPLAY, workingCategory3 = WorkingCategory3.FIXED
                    ))
                
            every { accountRepository.findByIdIn(any()) } returns listOf(createAccount(id = 100, externalKey = "A001"))

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
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = WorkingCategory3.FIXED)
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            ) } returns 1
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.save(match<MonthlyFemaleEmployeeIntegrationSchedule> {
                it.year == "2026" &&
                    it.month == "04" &&
                    it.workingDaysMonth?.compareTo(BigDecimal.ONE) == 0 &&
                    it.numberOfInputs?.compareTo(BigDecimal.ONE) == 0 &&
                    it.equivalentNumberOfWorkingDays?.compareTo(BigDecimal.ONE) == 0
            }) }
        }

        @Test
        @DisplayName("격고 환산 계산 - 격고 일정 1건 시 환산근무일수=0.5")
        fun refreshIntegration_alternateType() {
            // Given
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = WorkingCategory3.ALTERNATE)
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            ) } returns 1
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.save(match<MonthlyFemaleEmployeeIntegrationSchedule> {
                it.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("0.5")) == 0
            }) }
        }

        @Test
        @DisplayName("순회 환산 계산 - 3개 거래처 순회 시 환산근무일수=1/3")
        fun refreshIntegration_patrolType() {
            // Given
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 10), workingCategory3 = WorkingCategory3.PATROL)
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 10))
            ) } returns 3
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.save(match<MonthlyFemaleEmployeeIntegrationSchedule> {
                it.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("0.3333")) == 0
            }) }
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
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns existing

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.delete(existing) }
            verify(exactly = 0) { monthlyIntegrationScheduleRepository.save(any()) }
        }

        @Test
        @DisplayName("일정 0건 + 기존 레코드 없음 - 아무 작업도 하지 않음")
        fun noSchedules_noExisting() {
            // Given
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify(exactly = 0) { monthlyIntegrationScheduleRepository.delete(any()) }
            verify(exactly = 0) { monthlyIntegrationScheduleRepository.save(any()) }
        }

        @Test
        @DisplayName("환산인원 계산 - 환산근무일수/영업일수")
        fun convertedHeadcount_calculation() {
            // Given: 4월 영업일수 = 22일(평일) - 0일(공휴일) = 22일, 고정 10일 근무
            val schedules = (1..10).map { day ->
                createScheduleRecord(
                    id = day.toLong(), employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, day), workingCategory3 = WorkingCategory3.FIXED
                )
            }
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns schedules
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            schedules.forEach { s ->
                every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                    eq(employeeId), eq(s.workingDate!!)
                ) } returns 1
            }
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.save(match<MonthlyFemaleEmployeeIntegrationSchedule> {
                it.workingDaysMonth?.compareTo(BigDecimal("10")) == 0 &&
                    it.equivalentNumberOfWorkingDays?.compareTo(BigDecimal("10")) == 0 &&
                    it.convertedHeadcount?.compareTo(BigDecimal("0.4545")) == 0
            }) }
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
            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                createScheduleRecord(id = 1L, employeeId = employeeId, accountId = accountId,
                    workingDate = LocalDate.of(2026, 4, 1), workingCategory3 = WorkingCategory3.FIXED)
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns existing
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(LocalDate.of(2026, 4, 1))
            ) } returns 1
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            verify { monthlyIntegrationScheduleRepository.delete(existing) }
            verify { monthlyIntegrationScheduleRepository.save(any()) }
        }

        @Test
        @DisplayName("3필드 자동 set - workingCategory5='상시' + accountType 매칭 시 EICM lookup 결과 set (spec #680 §5.3 v1.3)")
        fun threeFieldAutoSet_withEicmLookup() {
            // Given
            val accountWithType = Account(id = accountId, accountType = AccountType.SUPER)
            val workingDate = LocalDate.of(2026, 4, 1)
            val dws = DisplayWorkSchedule(
                employee = Employee(id = employeeId, employeeCode = "EMP1", name = "테스트1"),
                account = accountWithType,
                confirmed = true,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31),
                typeOfWork5 = TypeOfWork5.REGULAR,
            )
            // entity AccountType.SUPER.displayName="수퍼" (legacy SF picklist) — AccountCategoryMaster.name 은
            // 운영에서 "슈퍼" (1글자 차이) 로 등록되어 있는 경우가 있음. mock 은 entity enum 값 그대로 사용.
            val mockCategory = AccountCategoryMaster(
                id = 35L,
                accountCode = "06",
                name = "수퍼",
            )
            val mockEicm = EmployeeInputCriteriaMaster(
                id = 8L,
                sfid = "a0o2x000001o2OjAAI",
                name = "IM-00000006",
                confirmed = true,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2023, 1, 1),
                category = mockCategory,
            )

            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                TeamMemberSchedule(
                    id = 1L,
                    employee = Employee(id = employeeId, employeeCode = "EMP1", name = "테스트1"),
                    account = accountWithType,
                    workingDate = workingDate,
                    workingType = WorkingType.WORK,
                    workingCategory1 = WorkingCategory1.DISPLAY,
                    workingCategory3 = WorkingCategory3.FIXED,
                    commuteLogSfid = "CL001",
                )
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(workingDate)
            ) } returns 1
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(any(), any()) } returns listOf(dws)
            every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns emptyList()
            every { accountCategoryMasterRepository.findByName("수퍼") } returns mockCategory
            every {
                employeeInputCriteriaMasterRepository.findActiveByCategoryAndTypeOfWork1(
                    eq(35L), eq(TypeOfWork1.DISPLAY), eq(LocalDate.of(2026, 4, 1))
                )
            } returns mockEicm
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then
            val captured = slot<MonthlyFemaleEmployeeIntegrationSchedule>()
            verify {
                monthlyIntegrationScheduleRepository.save(capture(captured))
            }
            // 3필드 set 검증 — workingCategory5='상시' 가드 통과 시
            val saved = captured.captured
            assertThat(saved.employeeInputCriteriaMaster?.id).isEqualTo(8L)
            assertThat(saved.employeeInputCriteriaMasterSfid).isEqualTo("a0o2x000001o2OjAAI")
            assertThat(saved.accountConvertedHeadcount).isNotNull
        }

        @Test
        @DisplayName("3필드 자동 set - workingCategory5='임시' (상시 아님) 인 경우 EICM lookup 호출 안 함 + 3필드 null")
        fun threeFieldAutoSet_nonRegular_noLookup() {
            // Given
            val workingDate = LocalDate.of(2026, 4, 1)
            val accountWithType = Account(id = accountId, accountType = AccountType.SUPER)
            val dws = DisplayWorkSchedule(
                employee = Employee(id = employeeId, employeeCode = "EMP1", name = "테스트1"),
                account = accountWithType,
                confirmed = true,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31),
                typeOfWork5 = TypeOfWork5.TEMPORARY, // 상시 아님
            )

            every { teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
                eq(employeeId), eq(accountId), any(), any()
            ) } returns listOf(
                TeamMemberSchedule(
                    id = 1L,
                    employee = Employee(id = employeeId, employeeCode = "EMP1", name = "테스트1"),
                    account = accountWithType,
                    workingDate = workingDate,
                    workingType = WorkingType.WORK,
                    workingCategory1 = WorkingCategory1.DISPLAY,
                    workingCategory3 = WorkingCategory3.FIXED,
                    commuteLogSfid = "CL001",
                )
            )
            every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
                eq(employeeId), eq(accountId), eq("2026"), eq("04")
            ) } returns null
            every { teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                eq(employeeId), eq(workingDate)
            ) } returns 1
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(any(), any()) } returns listOf(dws)
            every { monthlyIntegrationScheduleRepository.save(any<MonthlyFemaleEmployeeIntegrationSchedule>()) } answers { firstArg<MonthlyFemaleEmployeeIntegrationSchedule>() }

            // When
            service.refreshIntegration(employeeId, accountId, yearMonth)

            // Then — EICM lookup 미호출 + 3필드 null
            verify(exactly = 0) { accountCategoryMasterRepository.findByName(any()) }
            verify(exactly = 0) {
                employeeInputCriteriaMasterRepository.findActiveByCategoryAndTypeOfWork1(any(), any(), any())
            }
            verify {
                monthlyIntegrationScheduleRepository.save(match<MonthlyFemaleEmployeeIntegrationSchedule> {
                    it.employeeInputCriteriaMaster == null &&
                        it.employeeInputCriteriaMasterSfid == null &&
                        it.thisMonthAmount == null &&
                        it.accountConvertedHeadcount == null
                })
            }
        }
    }

    @Nested
    @DisplayName("calculateBusinessDays - 영업일수 계산")
    inner class CalculateBusinessDaysTests {

        @Test
        @DisplayName("공휴일 없는 월 - 평일 수만 반환")
        fun noHolidays() {
            // Given: 2026년 4월 = 평일 22일
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns emptyList()

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(22)
        }

        @Test
        @DisplayName("공휴일 있는 월 - 평일 공휴일 제외")
        fun withHolidays() {
            // Given: 2026년 4월, 4/1(수) 공휴일
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns listOf(
                HolidayMaster(
                    holidayDate = LocalDate.of(2026, 4, 1),
                    name = "테스트공휴일",
                    type = HolidayType.PUBLIC_HOLIDAY,
                    year = 2026
                )
            )

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(21)
        }

        @Test
        @DisplayName("주말 공휴일 - 이미 주말이라 영업일 감소 없음")
        fun weekendHoliday() {
            // Given: 2026년 4월, 4/4(토) 공휴일
            every { holidayMasterRepository.findByHolidayDateBetween(any(), any()) } returns listOf(
                HolidayMaster(
                    holidayDate = LocalDate.of(2026, 4, 4),
                    name = "토요일공휴일",
                    type = HolidayType.PUBLIC_HOLIDAY,
                    year = 2026
                )
            )

            // When
            val result = service.calculateBusinessDays(YearMonth.of(2026, 4))

            // Then
            assertThat(result).isEqualTo(22)
        }
    }

    @Nested
    @DisplayName("buildIntegrationItems - BranchCodeExpander 합성 (SF Util.getIncludedBranchCode 동등)")
    inner class BranchCodeExpansionTests {

        @Test
        @DisplayName("이력 코드 합집합 — BranchMapping seed 가 있는 코드 입력 시 IN 절이 이력 코드까지 확장")
        fun expandsWithBranchMappingHistory() {
            // Given: 입력 ["5849"] → 조직 계층 펼침 ["5849"] → BranchMapping (BC=5849, IBC="5479,5849") 합집합 ["5849","5479"]
            every { organizationRepository.expandCostCenterCodes(listOf("5849")) } returns listOf("5849")
            every { branchCodeExpander.expand(listOf("5849")) } returns setOf("5849", "5479")
            every { employeeRepository.findByCostCenterCodeInAndStatus(any(), any()) } returns emptyList()
            every { organizationRepository.searchForAdmin(any(), any(), any()) } returns emptyList()

            // When
            service.getMonthlyIntegration(2026, 5, listOf("5849"))

            // Then: BranchCodeExpander 결과 (["5849","5479"]) 로 employee 조회
            verify {
                employeeRepository.findByCostCenterCodeInAndStatus(
                    match { it.toSet() == setOf("5849", "5479") },
                    "재직"
                )
            }
        }

        @Test
        @DisplayName("합성 순서 — 조직 계층 펼침 (level3→level5) 후 BranchMapping 이력 합집합")
        fun composesOrgHierarchyThenBranchMapping() {
            // Given: 상위 코드 ["L3-CODE"] → 조직 펼침 ["5849","5750"] → BranchMapping (BC=5849 IBC="5479,5849") 합집합 ["5849","5750","5479"]
            every { organizationRepository.expandCostCenterCodes(listOf("L3-CODE")) } returns listOf("5849", "5750")
            every { branchCodeExpander.expand(listOf("5849", "5750")) } returns setOf("5849", "5750", "5479")
            every { employeeRepository.findByCostCenterCodeInAndStatus(any(), any()) } returns emptyList()
            every { organizationRepository.searchForAdmin(any(), any(), any()) } returns emptyList()

            // When
            service.getMonthlyIntegration(2026, 5, listOf("L3-CODE"))

            // Then: 조직 펼침 결과를 BranchCodeExpander 에 전달 + 최종 결과로 employee 조회
            verify { organizationRepository.expandCostCenterCodes(listOf("L3-CODE")) }
            verify { branchCodeExpander.expand(listOf("5849", "5750")) }
            verify {
                employeeRepository.findByCostCenterCodeInAndStatus(
                    match { it.toSet() == setOf("5849", "5750", "5479") },
                    "재직"
                )
            }
        }

        @Test
        @DisplayName("BranchMapping seed 미존재 — pass-through (회귀 없음)")
        fun passThroughWhenNoBranchMappingSeed() {
            // Given: 입력 ["9999"] → 조직 펼침 ["9999"] → BranchMapping seed 없음 → ["9999"] (자기 자신만)
            every { organizationRepository.expandCostCenterCodes(listOf("9999")) } returns listOf("9999")
            every { branchCodeExpander.expand(listOf("9999")) } returns setOf("9999")
            every { employeeRepository.findByCostCenterCodeInAndStatus(any(), any()) } returns emptyList()
            every { organizationRepository.searchForAdmin(any(), any(), any()) } returns emptyList()

            // When
            service.getMonthlyIntegration(2026, 5, listOf("9999"))

            // Then
            verify {
                employeeRepository.findByCostCenterCodeInAndStatus(
                    match { it.toSet() == setOf("9999") },
                    "재직"
                )
            }
        }

        @Test
        @DisplayName("확장 결과가 빈 집합 — 빈 결과 반환 (조기 종료)")
        fun returnsEmptyWhenExpandedCodesEmpty() {
            // Given: 조직 펼침은 비어있지 않으나 BranchCodeExpander 가 빈 집합 반환 (이론적 케이스)
            every { organizationRepository.expandCostCenterCodes(any()) } returns emptyList()
            every { branchCodeExpander.expand(emptyList()) } returns emptySet()

            // When
            val result = service.getMonthlyIntegration(2026, 5, listOf("CC001"))

            // Then: 조기 종료로 빈 결과
            assertThat(result.items).isEmpty()
            verify(exactly = 0) { employeeRepository.findByCostCenterCodeInAndStatus(any(), any()) }
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
        workingCategory1: WorkingCategory1 = WorkingCategory1.DISPLAY,
        workingCategory3: WorkingCategory3? = WorkingCategory3.FIXED,
        workingCategory4: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            employee = Employee(id = employeeId, employeeCode = "EMP$employeeId", name = "테스트$employeeId"),
            account = Account(id = accountId),
            workingDate = workingDate,
            workingType = WorkingType.WORK,
            workingCategory1 = workingCategory1,
            workingCategory3 = workingCategory3,
            workingCategory4 = workingCategory4,
            commuteLogSfid = "CL001"
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
