package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.exception.DateOutOfRangeException
import com.otoki.powersales.domain.activity.promotion.exception.DuplicateScheduleException
import com.otoki.powersales.domain.activity.promotion.exception.EmployeeOnLeaveException
import com.otoki.powersales.domain.activity.promotion.exception.EmployeeResignedException
import com.otoki.powersales.domain.activity.promotion.exception.LeaveConflictException
import com.otoki.powersales.domain.activity.promotion.exception.NoEmployeesException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.ValuesRequiredException
import com.otoki.powersales.domain.activity.promotion.exception.WorkType3LimitExceededException
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleOwnerResolver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@DisplayName("PromotionSchedulesUpsertHelper 테스트")
class PromotionSchedulesUpsertHelperTest {

    private val promotionRepository: PromotionRepository = mockk()
    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver = mockk()

    private val helper = PromotionSchedulesUpsertHelper(
        promotionRepository,
        promotionEmployeeRepository,
        teamMemberScheduleRepository,
        employeeRepository,
        teamMemberScheduleOwnerResolver,
    )

    init {
        every { teamMemberScheduleOwnerResolver.resolveOwnersByCostCenterCode(any()) } returns emptyMap()
    }

    private val startDate = LocalDate.of(2026, 3, 1)
    private val endDate = LocalDate.of(2026, 3, 31)

    @Nested
    @DisplayName("confirmPromotion - 행사 확정")
    inner class ConfirmPromotionTests {

        @Test
        @DisplayName("정상 확정 - PE 3명 신규 Upsert -> 200, 3건 생성")
        fun confirm_success_newInsert() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate),
                createPE(id = 2L, employeeId = 2L, scheduleDate = startDate.plus(1, ChronoUnit.DAYS)),
                createPE(id = 3L, employeeId = 3L, scheduleDate = startDate.plus(2, ChronoUnit.DAYS))
            )

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findByPromotionId(10L) } returns employees
            every { teamMemberScheduleRepository.findByPromotionEmployeeIn(any()) } returns emptyList()
            every { teamMemberScheduleRepository.findByEmployeeInAndWorkingDateIn(any(), any()) } returns emptyList()
            every { employeeRepository.findAllById(any()) } returns listOf(
                createEmployee(id = 1L, employeeCode = "EMP001", name = "김철수"),
                createEmployee(id = 2L, employeeCode = "EMP002", name = "이영희"),
                createEmployee(id = 3L, employeeCode = "EMP003", name = "박민수")
            )
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>().mapIndexed { index, s ->
                    TeamMemberSchedule(
                        id = (100L + index),
                        employee = s.employee,
                        account = s.account,
                        workingDate = s.workingDate,
                        workingType = s.workingType,
                        workingCategory1 = s.workingCategory1,
                        workingCategory3 = s.workingCategory3,
                        workingCategory4 = s.workingCategory4,
                        promotionEmployee = s.promotionEmployee
                    )
                }
            }
            every { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) } answers { firstArg<List<PromotionEmployee>>() }

            val result = helper.upsert(10L)

            assertThat(result.promotionId).isEqualTo(10L)
            assertThat(result.totalEmployees).isEqualTo(3)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(3)
            assertThat(employees[0].teamMemberScheduleId).isEqualTo(100L)
            assertThat(employees[1].teamMemberScheduleId).isEqualTo(101L)
            assertThat(employees[2].teamMemberScheduleId).isEqualTo(102L)
        }

        @Test
        @DisplayName("재확정 - 기존 스케줄 Upsert(UPDATE) -> 200")
        fun confirm_success_upsertUpdate() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate)
            )
            val existingTeamMemberSchedule = TeamMemberSchedule(
                id = 50L,
                employee = Employee(id = 1L, employeeCode = "EMP001", name = "테스트1"),
                account = Account(id = 100),
                workingDate = startDate,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                workingCategory3 = WorkingCategory3.FIXED,
                promotionEmployee = PromotionEmployee(id = 1L, promotionId = 10L)
            )

            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findByPromotionId(10L) } returns employees
            every { teamMemberScheduleRepository.findByPromotionEmployeeIn(any()) } returns listOf(existingTeamMemberSchedule)
            every { teamMemberScheduleRepository.findByEmployeeInAndWorkingDateIn(any(), any()) } returns listOf(existingTeamMemberSchedule)
            every { employeeRepository.findAllById(any()) } returns listOf(createEmployee(id = 1L, employeeCode = "EMP001", name = "김철수"))
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers { firstArg<List<TeamMemberSchedule>>() }
            every { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) } answers { firstArg<List<PromotionEmployee>>() }

            val result = helper.upsert(10L)

            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].teamMemberScheduleId).isEqualTo(50L)
        }

        @Test
        @DisplayName("행사 미존재 -> 404 PromotionNotFoundException")
        fun confirm_promotionNotFound() {
            every { promotionRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { helper.upsert(999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사 -> 404 PromotionNotFoundException")
        fun confirm_deletedPromotion() {
            val promotion = createPromotion(isDeleted = true)
            every { promotionRepository.findById(10L) } returns Optional.of(promotion)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("조원 0명 -> 400 NoEmployeesException")
        fun confirm_noEmployees() {
            val promotion = createPromotion()
            every { promotionRepository.findById(10L) } returns Optional.of(promotion)
            every { promotionEmployeeRepository.findByPromotionId(10L) } returns emptyList()

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(NoEmployeesException::class.java)
        }
    }

    @Nested
    @DisplayName("기본값 보정")
    inner class DefaultCorrectionTests {

        @Test
        @DisplayName("workType1 null -> '행사'로 보정 후 확정 성공")
        fun confirm_workType1Null_correctedToDefault() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, workType1 = null)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].workType1?.displayName).isEqualTo("행사")
        }

        @Test
        @DisplayName("workStatus null -> '근무'로 보정 후 확정 성공")
        fun confirm_workStatusNull_correctedToDefault() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, workStatus = null)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].workStatus?.displayName).isEqualTo("근무")
        }

        @Test
        @DisplayName("workType1/workStatus 모두 null -> 둘 다 보정 후 확정 성공")
        fun confirm_bothNull_correctedToDefaults() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, workType1 = null, workStatus = null)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].workType1?.displayName).isEqualTo("행사")
            assertThat(employees[0].workStatus?.displayName).isEqualTo("근무")
        }

        @Test
        @DisplayName("기존값 존재 -> 기존값 유지")
        fun confirm_existingValues_notOverridden() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, workType1 = "진열", workStatus = "연차")
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].workType1?.displayName).isEqualTo("진열")
            assertThat(employees[0].workStatus?.displayName).isEqualTo("연차")
        }
    }

    @Nested
    @DisplayName("employeeId 기반 확정")
    inner class EmployeeIdConfirmTests {

        @Test
        @DisplayName("employeeId 기반 사원 확정 -> 성공")
        fun confirm_employeeIdOnly_success() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
        }

        @Test
        @DisplayName("employeeId null -> 행사사원 누락 에러")
        fun confirm_noIdentifier_missingEmployee() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = null)
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(ValuesRequiredException::class.java)
                .hasMessageContaining("행사사원")
        }
    }

    @Nested
    @DisplayName("검증 1: 필수값 검증")
    inner class RequiredValuesTests {

        @Test
        @DisplayName("workType3 누락 -> 400 VALUES_REQUIRED (workType1은 보정됨)")
        fun confirm_missingWorkType3() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, workType1 = "", workType3 = "")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(ValuesRequiredException::class.java)
                .hasMessageContaining("근무유형3")
        }

        @Test
        @DisplayName("기준단가 누락 -> 400 VALUES_REQUIRED")
        fun confirm_missingBasePrice() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, basePrice = null)
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(ValuesRequiredException::class.java)
                .hasMessageContaining("기준단가")
        }

        @Test
        @DisplayName("목표수량 누락 -> 400 VALUES_REQUIRED")
        fun confirm_missingDailyTargetCount() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, dailyTargetCount = null)
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(ValuesRequiredException::class.java)
                .hasMessageContaining("목표수량")
        }

        @Test
        @DisplayName("목표금액 0/null -> 확정 성공 (SF 레거시 동등 — 목표금액은 확정 필수 아님)")
        fun confirm_zeroOrNullTargetAmount_success() {
            val promotion = createPromotion()
            // target_amount = 0 (SF 마이그레이션 적재분 NULL 과 동일하게 확정 검증 통과해야 함)
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, targetAmount = 0L)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
        }

        @Test
        @DisplayName("목표금액 null -> 확정 성공 (SF 마이그레이션 적재분 정합)")
        fun confirm_nullTargetAmount_success() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, targetAmount = null)
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("검증 2: 투입일 범위")
    inner class DateRangeTests {

        @Test
        @DisplayName("투입일이 행사 종료일 이후 -> 400 DATE_OUT_OF_RANGE")
        fun confirm_dateAfterEnd() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = endDate.plus(1, ChronoUnit.DAYS))
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(DateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("투입일이 행사 시작일 이전 -> 400 DATE_OUT_OF_RANGE")
        fun confirm_dateBeforeStart() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate.minus(1, ChronoUnit.DAYS))
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(DateOutOfRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("검증 3: 근무유형3 수량 제한")
    inner class WorkType3LimitTests {

        @Test
        @DisplayName("동일 사원+날짜에 고정 2개 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_fixed2() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "고정"),
                createPE(id = 2L, employeeId = 1L, scheduleDate = startDate, workType3 = "고정")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 고정+격고 혼합 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_fixedPlusAlternate() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "고정"),
                createPE(id = 2L, employeeId = 1L, scheduleDate = startDate, workType3 = "격고")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 3개 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_alternate3() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 2L, employeeId = 1L, scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 3L, employeeId = 1L, scheduleDate = startDate, workType3 = "격고")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 2 + 순회 추가 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_alternate2PlusTraversal() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = 1L, workingDate = startDate, workingCategory3 = WorkingCategory3.ALTERNATE),
                createTeamMemberSchedule(employeeId = 1L, workingDate = startDate, workingCategory3 = WorkingCategory3.ALTERNATE)
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 1 + 순회 1 -> 통과")
        fun confirm_alternate1PlusTraversal1_success() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 2L, employeeId = 1L, scheduleDate = startDate, workType3 = "순회")
            )
            setupMocksForSuccess(promotion, employees)

            val result = helper.upsert(10L)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("검증 4: 연차/대휴 충돌")
    inner class LeaveConflictTests {

        @Test
        @DisplayName("기존에 연차 스케줄 존재 -> 400 LEAVE_CONFLICT")
        fun confirm_existingLeave() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workStatus = "근무", workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = 1L, workingDate = startDate, workingType = WorkingType.ANNUAL_LEAVE, workingCategory3 = WorkingCategory3.PATROL)
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(LeaveConflictException::class.java)
        }

        @Test
        @DisplayName("PE가 연차인데 기존 근무 스케줄 존재 -> 400 LEAVE_CONFLICT")
        fun confirm_peLeaveExistingWork() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workStatus = "연차", workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = 1L, workingDate = startDate, workingType = WorkingType.WORK, workingCategory3 = WorkingCategory3.PATROL)
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(LeaveConflictException::class.java)
        }
    }

    @Nested
    @DisplayName("검증 5: 거래처 중복")
    inner class DuplicateScheduleTests {

        @Test
        @DisplayName("동일 거래처+사원+날짜 스케줄 존재 -> 400 DUPLICATE_SCHEDULE")
        fun confirm_duplicateTeamMemberSchedule() {
            val promotion = createPromotion(account = createAccount(id = 100))
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate, workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = 1L, workingDate = startDate, accountId = 100, workingCategory3 = WorkingCategory3.PATROL)
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(DuplicateScheduleException::class.java)
        }
    }

    @Nested
    @DisplayName("검증 6: 여사원 상태")
    inner class EmployeeStatusTests {

        @Test
        @DisplayName("여사원 휴직 -> 400 EMPLOYEE_ON_LEAVE")
        fun confirm_employeeOnLeave() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate)
            )
            setupMocks(promotion, employees, userStatus = "휴직")

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(EmployeeOnLeaveException::class.java)
        }

        @Test
        @DisplayName("여사원 퇴직 -> 400 EMPLOYEE_RESIGNED")
        fun confirm_employeeResigned() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeId = 1L, scheduleDate = startDate)
            )
            setupMocks(promotion, employees, userStatus = "퇴직")

            assertThatThrownBy { helper.upsert(10L) }
                .isInstanceOf(EmployeeResignedException::class.java)
        }
    }

    private fun setupMocks(
        promotion: Promotion,
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule> = emptyList(),
        userStatus: String? = null
    ) {
        every { promotionRepository.findById(10L) } returns Optional.of(promotion)
        every { promotionEmployeeRepository.findByPromotionId(10L) } returns employees
        every { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) } answers { firstArg<List<PromotionEmployee>>() }
        every { teamMemberScheduleRepository.findByPromotionEmployeeIn(any()) } returns emptyList()
        every { teamMemberScheduleRepository.findByEmployeeInAndWorkingDateIn(any(), any()) } returns existingTeamMemberSchedules

        val employeeIds = employees.mapNotNull { it.employeeId }.distinct()
        val empList = employeeIds.map { id ->
            createEmployee(id = id, employeeCode = "EMP${String.format("%03d", id)}", name = "EMP${String.format("%03d", id)}이름", status = userStatus)
        }
        every { employeeRepository.findAllById(any()) } returns empList
    }

    private fun setupMocksForSuccess(
        promotion: Promotion,
        employees: List<PromotionEmployee>
    ) {
        setupMocks(promotion, employees)
        every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
            firstArg<List<TeamMemberSchedule>>().mapIndexed { index, s ->
                TeamMemberSchedule(
                    id = (100L + index),
                    employee = s.employee,
                    account = s.account,
                    workingDate = s.workingDate,
                    workingType = s.workingType,
                    workingCategory1 = s.workingCategory1,
                    workingCategory3 = s.workingCategory3,
                    workingCategory4 = s.workingCategory4,
                    promotionEmployee = s.promotionEmployee
                )
            }
        }
        every { promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>()) } answers { firstArg<List<PromotionEmployee>>() }
    }

    private fun createPromotion(
        id: Long = 10L,
        account: Account = createAccount(),
        isDeleted: Boolean = false
    ): Promotion = Promotion(
        id = id,
        promotionNumber = "PRO-0001",
        account = account,
        startDate = startDate,
        endDate = endDate,
        isDeleted = isDeleted
    )

    private fun createPE(
        id: Long = 1L,
        promotionId: Long = 10L,
        employeeId: Long? = 1L,
        scheduleDate: LocalDate = startDate,
        workStatus: String? = "근무",
        workType1: String? = "행사",
        workType3: String? = "고정",
        basePrice: BigDecimal? = BigDecimal("1000"),
        dailyTargetCount: BigDecimal? = BigDecimal("10"),
        targetAmount: Long? = 10000L
    ): PromotionEmployee = PromotionEmployee(
        id = id,
        promotionId = promotionId,
        employeeId = employeeId,
        scheduleDate = scheduleDate,
        workStatus = workStatus?.takeIf { it.isNotBlank() }?.let { WorkingType.fromDisplayName(it) },
        workType1 = workType1?.takeIf { it.isNotBlank() }?.let { WorkingCategory1.fromDisplayName(it) },
        workType3 = workType3?.takeIf { it.isNotBlank() }?.let { WorkingCategory3.fromDisplayName(it) },
        basePrice = basePrice,
        dailyTargetCount = dailyTargetCount,
        targetAmount = targetAmount
    )

    private fun createAccount(
        id: Long = 100L,
        name: String? = "테스트거래처"
    ): Account = Account(
        id = id,
        name = name
    )

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "EMP001",
        name: String = "테스트사원",
        status: String? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        status = status
    )

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        employeeId: Long = 1L,
        workingDate: LocalDate = startDate,
        workingType: WorkingType = WorkingType.WORK,
        workingCategory3: WorkingCategory3 = WorkingCategory3.FIXED,
        accountId: Long? = 999,
        promotionEmployeeId: Long? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = Employee(id = employeeId, employeeCode = "EMP${String.format("%03d", employeeId)}", name = "테스트$employeeId"),
        workingDate = workingDate,
        workingType = workingType,
        workingCategory3 = workingCategory3,
        account = accountId?.let { Account(id = it) },
        promotionEmployee = promotionEmployeeId?.let { PromotionEmployee(id = it, promotionId = 10L) }
    )
}
