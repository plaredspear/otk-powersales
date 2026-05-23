package com.otoki.powersales.common.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.notice.repository.NoticeRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckTodayResponse
import com.otoki.powersales.safetycheck.service.SafetyCheckService
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import io.mockk.every
import io.mockk.mockk

@DisplayName("HomeService 테스트")
class HomeServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()

    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()

    private val noticeRepository: NoticeRepository = mockk()

    private val accountRepository: AccountRepository = mockk()

    private val safetyCheckService: SafetyCheckService = mockk()

    private val productExpirationRepository: ProductExpirationRepository = mockk()

    private val homeService = HomeService(
        employeeRepository,
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
        noticeRepository,
        accountRepository,
        safetyCheckService,
        productExpirationRepository,
    )

    @org.junit.jupiter.api.BeforeEach
    fun stubDefaultsForLenientCompatibility() {
        // 원본 테스트는 @MockitoSettings(LENIENT) 였으므로 unstubbed 메서드는 default 값 반환.
        // MockK strict 모드 호환을 위해 기본 stub 일괄 등록 (각 테스트에서 더 구체적인 stub 으로 override).
        every { productExpirationRepository.countByEmployeeIdAndAlarmDate(any(), any()) } returns 0L
        every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = false)
        every { noticeRepository.findRecentNotices(any()) } returns emptyList()
        every { accountRepository.findByIdIn(any()) } returns emptyList()
        every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
    }

    @Nested
    @DisplayName("getHomeData - 홈 화면 데이터 조회")
    inner class GetHomeDataTests {

        // ========== 역할별 스케줄 조회 ==========

        @Test
        @DisplayName("여사원 - 본인 스케줄만 조회 -> 본인 employeeId로 조회된 스케줄만 반환")
        fun user_onlyOwnSchedules() {
            // Given
            val userId = 1L
            val employeeCode = "20030117"
            val employee = createEmployee(id = userId, employeeCode = employeeCode, role = null)
            val account = createAccount(id = 8938, name = "이마트 부산점")

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.DISPLAY),
                createTeamMemberSchedule(id = 2L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns listOf(account)
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = false)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(2)
            assertThat(result.todaySchedules).allMatch { it.employeeCode == employeeCode }
            assertThat(result.todaySchedules[0].accountName).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("조장 - 팀 전체 스케줄 조회 -> orgName 기반 팀원 전체 스케줄 반환")
        fun leader_teamSchedules() {
            // Given
            val userId = 1L
            val member1Id = 2L
            val member2Id = 3L
            val leaderEmpNum = "00000001"
            val member1EmpNum = "00000002"
            val member2EmpNum = "00000003"
            val orgName = "부산1지점"

            val leader = createEmployee(id = userId, employeeCode = leaderEmpNum, orgName = orgName, role = AppAuthority.LEADER)
            val member1 = createEmployee(id = member1Id, employeeCode = member1EmpNum, orgName = orgName, name = "김영희")
            val member2 = createEmployee(id = member2Id, employeeCode = member2EmpNum, orgName = orgName, name = "박미나")
            val teamEmployees = listOf(leader, member1, member2)

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, employeeId = member1Id, accountId = 8938, workingCategory1 = WorkingCategory1.DISPLAY),
                createTeamMemberSchedule(id = 2L, employeeId = member2Id, accountId = 8939, workingCategory1 = WorkingCategory1.EVENT),
                createTeamMemberSchedule(id = 3L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.DISPLAY)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 부산점"),
                createAccount(id = 8939, name = "홈플러스 해운대점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(leader)
            every { employeeRepository.findByOrgName(orgName) } returns teamEmployees
            every { teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(any(), any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns accounts
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(3)
            val employeeCodes = result.todaySchedules.map { it.employeeCode }
            assertThat(employeeCodes).containsExactlyInAnyOrder(member1EmpNum, member2EmpNum, leaderEmpNum)
        }

        // ========== 안전점검 ==========

        @Test
        @DisplayName("여사원 안전점검 미완료 - 오늘 안전점검 미완료 -> safetyCheckRequired=true")
        fun user_safetyCheckNotCompleted() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = AppAuthority.WOMAN)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(userId) } returns SafetyCheckTodayResponse(completed = false)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.safetyCheckRequired).isTrue()
        }

        @Test
        @DisplayName("여사원 안전점검 완료 - 오늘 안전점검 완료 -> safetyCheckRequired=false")
        fun user_safetyCheckCompleted() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = AppAuthority.WOMAN)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(userId) } returns SafetyCheckTodayResponse(completed = true, submittedAt = LocalDateTime.now())
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.safetyCheckRequired).isFalse()
        }

        @Test
        @DisplayName("조장 안전점검 - 조장은 역할과 무관하게 -> safetyCheckRequired=false")
        fun leader_safetyCheckAlwaysFalse() {
            // Given
            val userId = 1L
            val leader = createEmployee(id = userId, orgName = "부산1지점", role = AppAuthority.LEADER)

            every { employeeRepository.findById(userId) } returns Optional.of(leader)
            every { employeeRepository.findByOrgName("부산1지점") } returns listOf(leader)
            every { teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.safetyCheckRequired).isFalse()
        }

        // ========== 유통기한 임박 알림 ==========

        @Test
        @DisplayName("유통기한 알림 - employeeId로 오늘 알람 3건 -> expiryCount=3, 프로필 정보 포함")
        fun expiryAlert_withCount() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()
            every { productExpirationRepository.countByEmployeeIdAndAlarmDate(userId, any()) } returns 3L

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(3)
            assertThat(result.expiryAlert!!.branchName).isEqualTo("부산1지점")
            assertThat(result.expiryAlert!!.employeeName).isEqualTo("최금주")
            assertThat(result.expiryAlert!!.employeeCode).isEqualTo("20030117")
        }

        @Test
        @DisplayName("유통기한 알림 0건 - 오늘 알람 0건 -> expiryAlert not null, expiryCount=0")
        fun expiryAlert_zeroCount() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()
            every { productExpirationRepository.countByEmployeeIdAndAlarmDate(userId, any()) } returns 0L

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(0)
        }

        @Test
        @DisplayName("유통기한 알림 - employeeId로 건수 조회 성공")
        fun expiryAlert_employeeId() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()
            every { productExpirationRepository.countByEmployeeIdAndAlarmDate(userId, any()) } returns 5L

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(5)
        }

        @Test
        @DisplayName("유통기한 알림 orgName null - orgName 없는 사용자 -> branchName 빈 문자열")
        fun expiryAlert_orgNameNull() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, orgName = null, role = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()
            every { productExpirationRepository.countByEmployeeIdAndAlarmDate(userId, any()) } returns 1L

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.branchName).isEqualTo("")
        }

        // ========== 정렬 ==========

        @Test
        @DisplayName("3단계 정렬 - 출근완료(0) -> 행사(1) -> 진열(2) 순서로 정렬")
        fun schedules_sortedByPriority() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            // 진열 (priority 2)
            val displaySchedule = createTeamMemberSchedule(
                id = 3L,
                employeeId = userId,
                accountId = 8938,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory2 = null,
                commuteLogSfid = null
            )
            // 행사 (priority 1)
            val eventSchedule = createTeamMemberSchedule(
                id = 2L,
                employeeId = userId,
                accountId = 8939,
                workingCategory1 = WorkingCategory1.EVENT,
                workingCategory2 = null,
                commuteLogSfid = null
            )
            // 출근완료 (priority 0)
            val commuteSchedule = createTeamMemberSchedule(
                id = 1L,
                employeeId = userId,
                accountId = 8941,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory2 = null,
                commuteLogSfid = "CLG001"
            )

            // 의도적으로 역순 전달 (진열 -> 행사 -> 출근완료)
            val teamMemberSchedules = listOf(displaySchedule, eventSchedule, commuteSchedule)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(3)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo(1L)
            assertThat(result.todaySchedules[1].scheduleId).isEqualTo(2L)
            assertThat(result.todaySchedules[2].scheduleId).isEqualTo(3L)
        }

        // ========== 중복 제거 ==========

        @Test
        @DisplayName("중복 제거 - 동일 id 스케줄이 복수 존재 -> 정렬 후 첫 번째만 반환")
        fun schedules_deduplicatedById() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            // 같은 id로 진열(priority 3)과 행사(priority 2) 스케줄 존재
            val displaySchedule = createTeamMemberSchedule(
                id = 1L,
                employeeId = userId,
                accountId = 8938,
                workingCategory1 = WorkingCategory1.DISPLAY,
                commuteLogSfid = null
            )
            val eventSchedule = createTeamMemberSchedule(
                id = 1L,
                employeeId = userId,
                accountId = 8939,
                workingCategory1 = WorkingCategory1.EVENT,
                commuteLogSfid = null
            )
            val otherSchedule = createTeamMemberSchedule(
                id = 2L,
                employeeId = userId,
                accountId = 8940,
                workingCategory1 = WorkingCategory1.DISPLAY,
                commuteLogSfid = null
            )

            val teamMemberSchedules = listOf(displaySchedule, eventSchedule, otherSchedule)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(2)
            val scheduleIds = result.todaySchedules.map { it.scheduleId }
            assertThat(scheduleIds).containsExactlyInAnyOrder(1L, 2L)
        }

        // ========== 출근현황 집계 ==========

        @Test
        @DisplayName("출근현황 집계 - 전체 건수와 출근 등록 건수 -> attendanceSummary 정확히 반환")
        fun attendanceSummary_correctCounts() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, employeeId = userId, accountId = 8938, commuteLogSfid = "CLG001"),
                createTeamMemberSchedule(id = 2L, employeeId = userId, accountId = 8939, commuteLogSfid = "CLG002"),
                createTeamMemberSchedule(id = 3L, employeeId = userId, accountId = 8940, commuteLogSfid = null)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns emptyList()
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.attendanceSummary.totalCount).isEqualTo(3)
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(2)
        }

        // ========== 에러 케이스 ==========

        // ========== 진열마스터 홈화면 포함 ==========

        @Test
        @DisplayName("진열마스터만 존재 - TMS 0건 + DWS 확정 1건 -> todaySchedules에 포함, 등록 버튼 활성화")
        fun displayWorkScheduleOnly_includedInSchedules() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            val account = createAccount(id = 742, name = "테스트 거래처")

            val displayWorkSchedule = createDisplayWorkSchedule(
                id = 100L,
                employeeId = userId,
                accountId = 742,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                typeOfWork3 = TypeOfWork3.ROTATION
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns listOf(displayWorkSchedule)
            every { accountRepository.findByIdIn(any()) } returns listOf(account)
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(1)
            assertThat(result.todaySchedules[0].displayWorkScheduleId).isEqualTo(100L)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo(0L)
            assertThat(result.todaySchedules[0].accountName).isEqualTo("테스트 거래처")
            assertThat(result.todaySchedules[0].workCategory).isEqualTo("진열")
            assertThat(result.todaySchedules[0].isCommuteRegistered).isFalse()
            assertThat(result.attendanceSummary.totalCount).isEqualTo(1)
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(0)
        }

        @Test
        @DisplayName("TMS + DWS 중복 거래처 - 같은 사원+거래처에 TMS와 DWS 모두 존재 -> DWS 제외, TMS만 반환")
        fun displayWorkSchedule_excludedWhenTmsExists() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            val account = createAccount(id = 742, name = "테스트 거래처")

            val teamMemberSchedule = createTeamMemberSchedule(
                id = 1L, employeeId = userId, accountId = 742, workingCategory1 = WorkingCategory1.DISPLAY
            )
            val displayWorkSchedule = createDisplayWorkSchedule(
                id = 100L, employeeId = userId, accountId = 742
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns listOf(teamMemberSchedule)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns listOf(displayWorkSchedule)
            every { accountRepository.findByIdIn(any()) } returns listOf(account)
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(1)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo(1L)
            assertThat(result.todaySchedules[0].displayWorkScheduleId).isNull()
        }

        // ========== 에러 케이스 ==========

        @Test
        @DisplayName("사용자 없음 - 존재하지 않는 userId -> UserNotFoundException 발생")
        fun userNotFound_throwsException() {
            // Given
            every { employeeRepository.findById(999L) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { homeService.getHomeData(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    // ========== Helper Factories ==========

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "20030117",
        name: String = "최금주",
        orgName: String? = "부산1지점",
        sfid: String? = null,
        role: String? = null
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            password = "encoded_password",
            name = name,
            orgName = orgName,
            sfid = sfid,
            role = role
        )
    }

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        sfid: String? = null,
        employeeId: Long? = null,
        accountId: Int? = null,
        workingDate: LocalDate = LocalDate.now(),
        workingType: WorkingType? = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.DISPLAY,
        workingCategory2: WorkingCategory2? = null,
        commuteLogSfid: String? = null,
        commuteReportDatetime: LocalDateTime? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employee = employeeId?.let { Employee(id = it, employeeCode = "EMP$it", name = "테스트$it") },
            account = accountId?.let { Account(id = it) },
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
            workingCategory2 = workingCategory2,
            commuteLogSfid = commuteLogSfid,
            // Spec #789 정합 — 출근 등록 가드는 attendance_log id-FK 기준.
            attendanceLog = commuteLogSfid?.let { com.otoki.powersales.schedule.entity.AttendanceLog(id = 1L) },
            commuteReportDatetime = commuteReportDatetime
        )
    }

    private fun createDisplayWorkSchedule(
        id: Long = 0L,
        employeeId: Long? = null,
        accountId: Int? = null,
        typeOfWork1: TypeOfWork1? = TypeOfWork1.DISPLAY,
        typeOfWork3: TypeOfWork3? = TypeOfWork3.ROTATION,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            id = id,
            employee = employeeId?.let { Employee(id = it, employeeCode = "EMP$it", name = "테스트$it") },
            account = accountId?.let { Account(id = it) },
            typeOfWork1 = typeOfWork1,
            typeOfWork3 = typeOfWork3,
            startDate = startDate,
            endDate = endDate,
            confirmed = true,
            isDeleted = false
        )
    }

    private fun createAccount(
        id: Int = 0,
        sfid: String? = null,
        name: String? = null
    ): Account {
        return Account(
            id = id,
            sfid = sfid,
            name = name
        )
    }
}
