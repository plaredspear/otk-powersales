package com.otoki.internal.admin.service

import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
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
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionConfirmService 테스트")
class AdminPromotionConfirmServiceTest {

    @Mock private lateinit var promotionRepository: PromotionRepository
    @Mock private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @Mock private lateinit var userRepository: UserRepository
    @InjectMocks private lateinit var service: AdminPromotionConfirmService

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
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate),
                createPE(id = 2L, employeeSfid = "EMP002", scheduleDate = startDate.plusDays(1)),
                createPE(id = 3L, employeeSfid = "EMP003", scheduleDate = startDate.plusDays(2))
            )

            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.findByPromotionId(10L)).thenReturn(employees)
            whenever(teamMemberScheduleRepository.findByPromotionEmpIdExtIn(any())).thenReturn(emptyList())
            whenever(teamMemberScheduleRepository.findByEmployeeIdInAndWorkingDateIn(any(), any())).thenReturn(emptyList())
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(
                createUser("EMP001", "김철수"),
                createUser("EMP002", "이영희"),
                createUser("EMP003", "박민수")
            ))
            whenever(teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>())).thenAnswer { invocation ->
                val teamMemberSchedules = invocation.getArgument<List<TeamMemberSchedule>>(0)
                teamMemberSchedules.mapIndexed { index, s ->
                    TeamMemberSchedule(
                        id = (100L + index),
                        employeeId = s.employeeId,
                        accountId = s.accountId,
                        workingDate = s.workingDate,
                        workingType = s.workingType,
                        workingCategory1 = s.workingCategory1,
                        workingCategory3 = s.workingCategory3,
                        workingCategory4 = s.workingCategory4,
                        promotionEmpId = s.promotionEmpId,
                        promotionEmpIdExt = s.promotionEmpIdExt
                    )
                }
            }
            whenever(promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>())).thenAnswer { it.getArgument<List<PromotionEmployee>>(0) }

            val result = service.confirmPromotion(10L)

            assertThat(result.promotionId).isEqualTo(10L)
            assertThat(result.totalEmployees).isEqualTo(3)
            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(3)
            assertThat(employees[0].scheduleId).isEqualTo(100L)
            assertThat(employees[1].scheduleId).isEqualTo(101L)
            assertThat(employees[2].scheduleId).isEqualTo(102L)
        }

        @Test
        @DisplayName("재확정 - 기존 스케줄 Upsert(UPDATE) -> 200")
        fun confirm_success_upsertUpdate() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate)
            )
            val existingTeamMemberSchedule = TeamMemberSchedule(
                id = 50L,
                employeeId = "EMP001",
                accountId = "100",
                workingDate = startDate,
                workingType = "근무",
                workingCategory1 = "행사",
                workingCategory3 = "고정",
                promotionEmpId = "1",
                promotionEmpIdExt = "1"
            )

            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.findByPromotionId(10L)).thenReturn(employees)
            whenever(teamMemberScheduleRepository.findByPromotionEmpIdExtIn(listOf("1"))).thenReturn(listOf(existingTeamMemberSchedule))
            whenever(teamMemberScheduleRepository.findByEmployeeIdInAndWorkingDateIn(any(), any())).thenReturn(listOf(existingTeamMemberSchedule))
            whenever(userRepository.findBySfidIn(any())).thenReturn(listOf(createUser("EMP001", "김철수")))
            whenever(teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>())).thenAnswer { it.getArgument<List<TeamMemberSchedule>>(0) }
            whenever(promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>())).thenAnswer { it.getArgument<List<PromotionEmployee>>(0) }

            val result = service.confirmPromotion(10L)

            assertThat(result.upsertedTeamMemberSchedules).isEqualTo(1)
            assertThat(employees[0].scheduleId).isEqualTo(50L)
        }

        @Test
        @DisplayName("행사 미존재 -> 404 PromotionNotFoundException")
        fun confirm_promotionNotFound() {
            whenever(promotionRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.confirmPromotion(999L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 행사 -> 404 PromotionNotFoundException")
        fun confirm_deletedPromotion() {
            val promotion = createPromotion(isDeleted = true)
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(PromotionNotFoundException::class.java)
        }

        @Test
        @DisplayName("조원 0명 -> 400 NoEmployeesException")
        fun confirm_noEmployees() {
            val promotion = createPromotion()
            whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
            whenever(promotionEmployeeRepository.findByPromotionId(10L)).thenReturn(emptyList())

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(NoEmployeesException::class.java)
        }
    }

    @Nested
    @DisplayName("검증 1: 필수값 검증")
    inner class RequiredValuesTests {

        @Test
        @DisplayName("work_type1 누락 -> 400 VALUES_REQUIRED")
        fun confirm_missingWorkType1() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", workType1 = "")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(ValuesRequiredException::class.java)
                .hasMessageContaining("work_type1")
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
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = endDate.plusDays(1))
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(DateOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("투입일이 행사 시작일 이전 -> 400 DATE_OUT_OF_RANGE")
        fun confirm_dateBeforeStart() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate.minusDays(1))
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
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
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "고정"),
                createPE(id = 2L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "고정")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 고정+격고 혼합 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_fixedPlusAlternate() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "고정"),
                createPE(id = 2L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "격고")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 3개 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_alternate3() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 2L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 3L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "격고")
            )
            setupMocks(promotion, employees)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 2 + 순회 추가 -> 400 WORK_TYPE3_LIMIT_EXCEEDED")
        fun confirm_alternate2PlusTraversal() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = "EMP001", workingDate = startDate, workingCategory3 = "격고"),
                createTeamMemberSchedule(employeeId = "EMP001", workingDate = startDate, workingCategory3 = "격고")
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(WorkType3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("동일 사원+날짜에 격고 1 + 순회 1 -> 통과")
        fun confirm_alternate1PlusTraversal1_success() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "격고"),
                createPE(id = 2L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "순회")
            )
            setupMocksForSuccess(promotion, employees)

            val result = service.confirmPromotion(10L)
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
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workStatus = "근무", workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = "EMP001", workingDate = startDate, workingType = "연차", workingCategory3 = "순회")
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(LeaveConflictException::class.java)
        }

        @Test
        @DisplayName("PE가 연차인데 기존 근무 스케줄 존재 -> 400 LEAVE_CONFLICT")
        fun confirm_peLeaveExistingWork() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workStatus = "연차", workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = "EMP001", workingDate = startDate, workingType = "근무", workingCategory3 = "순회")
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(LeaveConflictException::class.java)
        }
    }

    @Nested
    @DisplayName("검증 5: 거래처 중복")
    inner class DuplicateScheduleTests {

        @Test
        @DisplayName("동일 거래처+사원+날짜 스케줄 존재 -> 400 DUPLICATE_SCHEDULE")
        fun confirm_duplicateTeamMemberSchedule() {
            val promotion = createPromotion(accountId = 100)
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate, workType3 = "순회")
            )
            val existingTeamMemberSchedules = listOf(
                createTeamMemberSchedule(employeeId = "EMP001", workingDate = startDate, accountId = "100", workingCategory3 = "순회")
            )
            setupMocks(promotion, employees, existingTeamMemberSchedules = existingTeamMemberSchedules)

            assertThatThrownBy { service.confirmPromotion(10L) }
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
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate)
            )
            setupMocks(promotion, employees, userStatus = "휴직")

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(EmployeeOnLeaveException::class.java)
        }

        @Test
        @DisplayName("여사원 퇴직 -> 400 EMPLOYEE_RESIGNED")
        fun confirm_employeeResigned() {
            val promotion = createPromotion()
            val employees = listOf(
                createPE(id = 1L, employeeSfid = "EMP001", scheduleDate = startDate)
            )
            setupMocks(promotion, employees, userStatus = "퇴직")

            assertThatThrownBy { service.confirmPromotion(10L) }
                .isInstanceOf(EmployeeResignedException::class.java)
        }
    }

    // --- Helper Methods ---

    private fun setupMocks(
        promotion: Promotion,
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule> = emptyList(),
        userStatus: String? = null
    ) {
        whenever(promotionRepository.findById(10L)).thenReturn(Optional.of(promotion))
        whenever(promotionEmployeeRepository.findByPromotionId(10L)).thenReturn(employees)
        whenever(teamMemberScheduleRepository.findByPromotionEmpIdExtIn(any())).thenReturn(emptyList())
        whenever(teamMemberScheduleRepository.findByEmployeeIdInAndWorkingDateIn(any(), any())).thenReturn(existingTeamMemberSchedules)

        val sfids = employees.mapNotNull { it.employeeSfid }.distinct()
        val users = sfids.map { createUser(it, "${it}이름", userStatus) }
        whenever(userRepository.findBySfidIn(any())).thenReturn(users)
    }

    private fun setupMocksForSuccess(
        promotion: Promotion,
        employees: List<PromotionEmployee>
    ) {
        setupMocks(promotion, employees)
        whenever(teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>())).thenAnswer { invocation ->
            val schedules = invocation.getArgument<List<TeamMemberSchedule>>(0)
            schedules.mapIndexed { index, s ->
                TeamMemberSchedule(
                    id = (100L + index),
                    employeeId = s.employeeId,
                    accountId = s.accountId,
                    workingDate = s.workingDate,
                    workingType = s.workingType,
                    workingCategory1 = s.workingCategory1,
                    workingCategory3 = s.workingCategory3,
                    workingCategory4 = s.workingCategory4,
                    promotionEmpId = s.promotionEmpId,
                    promotionEmpIdExt = s.promotionEmpIdExt
                )
            }
        }
        whenever(promotionEmployeeRepository.saveAll(any<List<PromotionEmployee>>())).thenAnswer { it.getArgument<List<PromotionEmployee>>(0) }
    }

    private fun createPromotion(
        id: Long = 10L,
        accountId: Int = 100,
        isDeleted: Boolean = false
    ): Promotion = Promotion(
        id = id,
        promotionNumber = "PRO-0001",
        promotionName = "테스트행사",
        accountId = accountId,
        startDate = startDate,
        endDate = endDate,
        isDeleted = isDeleted
    )

    private fun createPE(
        id: Long = 1L,
        promotionId: Long = 10L,
        employeeSfid: String = "EMP001",
        scheduleDate: LocalDate = startDate,
        workStatus: String = "근무",
        workType1: String = "행사",
        workType3: String = "고정",
        workType4: String? = null
    ): PromotionEmployee = PromotionEmployee(
        id = id,
        promotionId = promotionId,
        employeeSfid = employeeSfid,
        scheduleDate = scheduleDate,
        workStatus = workStatus,
        workType1 = workType1,
        workType3 = workType3,
        workType4 = workType4
    )

    private fun createUser(
        sfid: String = "EMP001",
        name: String = "테스트사원",
        status: String? = null
    ): User = User(
        sfid = sfid,
        employeeId = sfid.takeLast(8).padStart(8, '0'),
        name = name,
        status = status
    )

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        employeeId: String = "EMP001",
        workingDate: LocalDate = startDate,
        workingType: String = "근무",
        workingCategory3: String = "고정",
        accountId: String = "999",
        promotionEmpIdExt: String? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employeeId = employeeId,
        workingDate = workingDate,
        workingType = workingType,
        workingCategory3 = workingCategory3,
        accountId = accountId,
        promotionEmpIdExt = promotionEmpIdExt
    )
}
