package com.otoki.internal.admin.service

import com.otoki.internal.admin.exception.TeamScheduleEmployeeNotFoundException
import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import com.otoki.internal.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.internal.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.EmployeeRepository
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminSafetyCheckService 테스트")
class AdminSafetyCheckServiceTest {

    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @Mock private lateinit var safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository
    @Mock private lateinit var safetyCheckItemRepository: SafetyCheckItemRepository
    @Mock private lateinit var accountRepository: AccountRepository

    @InjectMocks private lateinit var service: AdminSafetyCheckService

    private val today = LocalDate.of(2026, 3, 17)
    private val adminUserId = 1L

    @BeforeEach
    fun setUp() {
        whenever(safetyCheckItemRepository.findByUseYnOrderByQuestionNumAscSeqNumAsc("Y"))
            .thenReturn(listOf(
                createSafetyCheckItem(1, 1, "손목보호대 착용"),
                createSafetyCheckItem(1, 2, "숨수건(화재피해 예방) 소지"),
                createSafetyCheckItem(1, 3, "안전화 착용"),
                createSafetyCheckItem(1, 4, "진열업무시 코팅장갑 및 허리보호대 착용"),
                createSafetyCheckItem(1, 5, "진열대가 높을 경우 안전사다리 사용"),
                createSafetyCheckItem(1, 6, "시식행사 진행시 위생장갑 사용"),
                createSafetyCheckItem(1, 7, "오뚜기 유니폼 착용"),
                createSafetyCheckItem(1, 8, "오뚜기 판매여사원 명찰 착용"),
                createSafetyCheckItem(1, 9, "(위생)마스크 착용")
            ))
        service.initEquipmentLabels()
    }

    @Nested
    @DisplayName("getStatus - 안전점검 현황 조회")
    inner class GetStatusTests {

        @Test
        @DisplayName("정상 조회 - 일부 제출, 일부 미제출")
        fun getStatus_partialSubmission() {
            // Given
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member1 = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")
            val member2 = createEmployee(55L, "654321", "김영희", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member1, member2))

            val schedule1 = createSchedule(1L, 42L, today, "근무", accountId = 100)
            val schedule2 = createSchedule(2L, 55L, today, "근무", accountId = 200)
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(schedule1, schedule2))

            val submission = createSubmission(42L, today)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(listOf(submission))

            val account1 = createAccount(100, "이마트 강남점")
            val account2 = createAccount(200, "홈플러스 역삼점")
            whenever(accountRepository.findByIdIn(any())).thenReturn(listOf(account1, account2))

            // When
            val result = service.getStatus(adminUserId, today)

            // Then
            assertThat(result.date).isEqualTo("2026-03-17")
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.submittedCount).isEqualTo(1)
            assertThat(result.notSubmittedCount).isEqualTo(1)
            assertThat(result.members).hasSize(2)

            val submitted = result.members.first { it.submitted }
            assertThat(submitted.employeeCode).isEqualTo("123456")
            assertThat(submitted.employeeName).isEqualTo("홍길동")
            assertThat(submitted.accountName).isEqualTo("이마트 강남점")
            assertThat(submitted.equipments).hasSize(9)
            assertThat(submitted.equipments[0].seqNum).isEqualTo(1)
            assertThat(submitted.equipments[0].label).isEqualTo("손목보호대 착용")
            assertThat(submitted.equipments[0].answer).isEqualTo("예")
            assertThat(submitted.yesCount).isEqualTo(7)
            assertThat(submitted.noCount).isEqualTo(2)

            val notSubmitted = result.members.first { !it.submitted }
            assertThat(notSubmitted.employeeCode).isEqualTo("654321")
            assertThat(notSubmitted.equipments).isEmpty()
            assertThat(notSubmitted.yesCount).isEqualTo(0)
        }

        @Test
        @DisplayName("오늘 날짜 기본 조회 - date 미지정 시 오늘 기준")
        fun getStatus_defaultToday() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(emptyList())

            val result = service.getStatus(adminUserId, LocalDate.now())

            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.members).isEmpty()
        }

        @Test
        @DisplayName("전원 제출 완료")
        fun getStatus_allSubmitted() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member1 = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")
            val member2 = createEmployee(55L, "654321", "김영희", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member1, member2))

            val schedule1 = createSchedule(1L, 42L, today, "근무")
            val schedule2 = createSchedule(2L, 55L, today, "근무")
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(schedule1, schedule2))

            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(listOf(
                    createSubmission(42L, today),
                    createSubmission(55L, today)
                ))

            val result = service.getStatus(adminUserId, today)

            assertThat(result.submittedCount).isEqualTo(2)
            assertThat(result.notSubmittedCount).isEqualTo(0)
            assertThat(result.members).allMatch { it.submitted }
        }

        @Test
        @DisplayName("스케줄 없는 날짜 - 대상 인원 0명")
        fun getStatus_noSchedules() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member))
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(emptyList())

            val result = service.getStatus(adminUserId, today)

            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.members).isEmpty()
        }

        @Test
        @DisplayName("연차 스케줄 제외 - 근무 유형만 대상")
        fun getStatus_excludeNonWorkSchedules() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member1 = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")
            val member2 = createEmployee(55L, "654321", "김영희", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member1, member2))

            val workSchedule = createSchedule(1L, 42L, today, "근무")
            val leaveSchedule = createSchedule(2L, 55L, today, "연차")
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(workSchedule, leaveSchedule))

            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(emptyList())

            val result = service.getStatus(adminUserId, today)

            assertThat(result.totalCount).isEqualTo(1)
            assertThat(result.members).hasSize(1)
            assertThat(result.members[0].employeeCode).isEqualTo("123456")
        }

        @Test
        @DisplayName("복수 스케줄 - traversalFlag 'O' 우선")
        fun getStatus_multipleSchedules_traversalFlagPriority() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member))

            val schedule1 = createSchedule(1L, 42L, today, "근무", accountId = 100, traversalFlag = null)
            val schedule2 = createSchedule(2L, 42L, today, "근무", accountId = 200, traversalFlag = "O")
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(schedule1, schedule2))

            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(emptyList())

            val account = createAccount(200, "홈플러스 역삼점")
            whenever(accountRepository.findByIdIn(any())).thenReturn(listOf(account))

            val result = service.getStatus(adminUserId, today)

            assertThat(result.members).hasSize(1)
            assertThat(result.members[0].accountName).isEqualTo("홈플러스 역삼점")
        }

        @Test
        @DisplayName("costCenterCode 없는 사용자 - 빈 결과")
        fun getStatus_noCostCenterCode() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", null)
            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))

            val result = service.getStatus(adminUserId, today)

            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.members).isEmpty()
        }

        @Test
        @DisplayName("사용자 없음 - TeamScheduleEmployeeNotFoundException")
        fun getStatus_userNotFound() {
            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.getStatus(adminUserId, today) }
                .isInstanceOf(TeamScheduleEmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 여사원 제외")
        fun getStatus_excludeDeletedMembers() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val activeMember = createEmployee(42L, "123456", "홍길동", "여사원", "CC001")
            val deletedMember = createEmployee(55L, "654321", "김영희", "여사원", "CC001", isDeleted = true)

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(activeMember, deletedMember))

            val schedule = createSchedule(1L, 42L, today, "근무")
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(schedule))
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(emptyList())

            val result = service.getStatus(adminUserId, today)

            assertThat(result.totalCount).isEqualTo(1)
        }

        @Test
        @DisplayName("결과 정렬 - 사원명 가나다순")
        fun getStatus_sortedByName() {
            val admin = createEmployee(adminUserId, "10000001", "관리자", "조장", "CC001")
            val member1 = createEmployee(42L, "111111", "홍길동", "여사원", "CC001")
            val member2 = createEmployee(55L, "222222", "김영희", "여사원", "CC001")
            val member3 = createEmployee(66L, "333333", "박민수", "여사원", "CC001")

            whenever(employeeRepository.findById(adminUserId)).thenReturn(Optional.of(admin))
            whenever(employeeRepository.findByCostCenterCodeAndAppAuthority("CC001", "여사원"))
                .thenReturn(listOf(member1, member2, member3))

            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(eq(today), any()))
                .thenReturn(listOf(
                    createSchedule(1L, 42L, today, "근무"),
                    createSchedule(2L, 55L, today, "근무"),
                    createSchedule(3L, 66L, today, "근무")
                ))
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), eq(today)))
                .thenReturn(emptyList())

            val result = service.getStatus(adminUserId, today)

            assertThat(result.members.map { it.employeeName })
                .containsExactly("김영희", "박민수", "홍길동")
        }
    }

    // --- Helper factory methods ---

    private fun createEmployee(
        id: Long,
        employeeCode: String,
        name: String,
        appAuthority: String?,
        costCenterCode: String?,
        isDeleted: Boolean? = null
    ): Employee {
        val employee = Employee(id = id, employeeCode = employeeCode, name = name)
        employee.appAuthority = appAuthority
        employee.costCenterCode = costCenterCode
        val isDeletedField = Employee::class.java.getDeclaredField("isDeleted")
        isDeletedField.isAccessible = true
        isDeletedField.set(employee, isDeleted)
        return employee
    }

    private fun createSchedule(
        id: Long,
        employeeId: Long,
        workingDate: LocalDate,
        workingType: String,
        accountId: Int? = null,
        traversalFlag: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            employeeId = employeeId,
            workingDate = workingDate,
            workingType = workingType,
            accountId = accountId,
            traversalFlag = traversalFlag
        )
    }

    private fun createSubmission(
        employeeId: Long,
        workingDate: LocalDate
    ): SafetyCheckSubmission {
        return SafetyCheckSubmission(
            displayWorkScheduleId = 1L,
            employeeId = employeeId,
            workingDate = workingDate,
            completeTime = LocalDateTime.of(2026, 3, 17, 9, 15, 30),
            yesCheckCount = 7,
            noCheckCount = 2,
            equipment1 = "예",
            equipment2 = "예",
            equipment3 = "예",
            equipment4 = "해당없음",
            equipment5 = "해당없음",
            equipment6 = "예",
            equipment7 = "예",
            equipment8 = "예",
            equipment9 = "예",
            precaution = "매장 내 안전사고 유의;중량물 취급 시 주의",
            precautionCheckCount = 2
        )
    }

    private fun createSafetyCheckItem(questionNum: Int, seqNum: Int, contents: String): SafetyCheckItem {
        return SafetyCheckItem(questionNum = questionNum, seqNum = seqNum, contents = contents)
    }

    private fun createAccount(id: Int, name: String): Account {
        return Account(id = id, name = name)
    }
}
