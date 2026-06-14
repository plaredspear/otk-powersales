package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.safetycheck.dto.response.SafetyCheckTodayResponse
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.domain.activity.safetycheck.service.SafetyCheckService
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.schedule.entity.AttendanceLog
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
import org.junit.jupiter.api.BeforeEach

@DisplayName("HomeService 테스트")
class HomeServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()

    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()

    private val noticeRepository: NoticeRepository = mockk()

    private val accountRepository: AccountRepository = mockk()

    private val safetyCheckService: SafetyCheckService = mockk()

    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository = mockk()

    private val productExpirationRepository: ProductExpirationRepository = mockk()

    private val homeService = HomeService(
        employeeRepository,
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
        noticeRepository,
        accountRepository,
        safetyCheckService,
        safetyCheckSubmissionRepository,
        productExpirationRepository,
    )

    @BeforeEach
    fun stubDefaultsForLenientCompatibility() {
        // 원본 테스트는 @MockitoSettings(LENIENT) 였으므로 unstubbed 메서드는 default 값 반환.
        // MockK strict 모드 호환을 위해 기본 stub 일괄 등록 (각 테스트에서 더 구체적인 stub 으로 override).
        every { productExpirationRepository.countByEmployeeIdAndAlarmDate(any(), any()) } returns 0L
        every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = false)
        every { noticeRepository.findRecentNotices(any()) } returns emptyList()
        every { accountRepository.findByIdIn(any()) } returns emptyList()
        every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
        every { safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), any()) } returns emptyList()
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
                createTeamMemberSchedule(id = 1L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT),
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

        // ========== 출근/근태 영역 노출 대상 여부 (attendanceApplicable) ==========

        @Test
        @DisplayName("여사원 - attendanceApplicable=true (본인 출근 등록 노출)")
        fun attendanceApplicable_woman_true() {
            val userId = 1L
            val employee = createEmployee(id = userId, role = AppAuthority.WOMAN)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns emptyList()

            val result = homeService.getHomeData(userId)

            assertThat(result.attendanceApplicable).isTrue()
        }

        @Test
        @DisplayName("조장 - attendanceApplicable=true (팀 출근 현황 노출)")
        fun attendanceApplicable_leader_true() {
            val userId = 1L
            val leader = createEmployee(id = userId, orgName = "부산1지점", role = AppAuthority.LEADER)
            every { employeeRepository.findById(userId) } returns Optional.of(leader)
            every { employeeRepository.findByOrgName("부산1지점") } returns listOf(leader)
            every { teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(any(), any()) } returns emptyList()

            val result = homeService.getHomeData(userId)

            assertThat(result.attendanceApplicable).isTrue()
        }

        @Test
        @DisplayName("지점장 - attendanceApplicable=false (본인 스케줄이 잡혀도 근태 영역 비노출)")
        fun attendanceApplicable_branchManager_false() {
            val userId = 1L
            val manager = createEmployee(id = userId, role = AppAuthority.BRANCH_MANAGER)
            every { employeeRepository.findById(userId) } returns Optional.of(manager)
            // 지점장 본인 스케줄이 잡혀 있어도(else 분기) 근태 영역은 노출하지 않는다.
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns
                listOf(createTeamMemberSchedule(id = 1L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT))
            every { accountRepository.findByIdIn(any()) } returns listOf(createAccount(id = 8938, name = "이마트 부산점"))

            val result = homeService.getHomeData(userId)

            assertThat(result.attendanceApplicable).isFalse()
        }

        @Test
        @DisplayName("role 미매핑(null) - attendanceApplicable=false")
        fun attendanceApplicable_nullRole_false() {
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns emptyList()

            val result = homeService.getHomeData(userId)

            assertThat(result.attendanceApplicable).isFalse()
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
                createTeamMemberSchedule(id = 1L, employeeId = member1Id, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT),
                createTeamMemberSchedule(id = 2L, employeeId = member2Id, accountId = 8939, workingCategory1 = WorkingCategory1.EVENT),
                createTeamMemberSchedule(id = 3L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT)
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
        @DisplayName("정렬 - 출근완료 행사 -> 미출근 행사 -> 진열(마스터) 순서로 정렬")
        fun schedules_sortedByPriority() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            val account = createAccount(id = 8940, name = "진열 거래처")

            // 미출근 행사 (priority 1)
            val eventSchedule = createTeamMemberSchedule(
                id = 2L,
                employeeId = userId,
                accountId = 8939,
                workingCategory1 = WorkingCategory1.EVENT,
                workingCategory2 = null,
                commuteLogSfid = null
            )
            // 출근완료 행사 (priority 0)
            val commuteSchedule = createTeamMemberSchedule(
                id = 1L,
                employeeId = userId,
                accountId = 8941,
                workingCategory1 = WorkingCategory1.EVENT,
                workingCategory2 = null,
                commuteLogSfid = "CLG001"
            )

            // 진열: 확정 마스터로만 집계 (레거시 정합) — 항상 행사 뒤에 정렬
            val displayMaster = createDisplayWorkSchedule(id = 300L, employeeId = userId, accountId = 8940)

            // 의도적으로 역순 전달 (미출근 행사 -> 출근완료 행사)
            val teamMemberSchedules = listOf(eventSchedule, commuteSchedule)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns listOf(displayMaster)
            every { accountRepository.findByIdIn(any()) } returns listOf(account)
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = true)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(3)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo(1L) // 출근완료 행사
            assertThat(result.todaySchedules[1].scheduleId).isEqualTo(2L) // 미출근 행사
            assertThat(result.todaySchedules[2].displayWorkScheduleId).isEqualTo(300L) // 진열 마스터
        }

        // ========== 중복 제거 ==========

        @Test
        @DisplayName("중복 제거 - 동일 id 스케줄이 복수 존재 -> 정렬 후 첫 번째만 반환")
        fun schedules_deduplicatedById() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)

            // 같은 id로 행사 스케줄 2건(중복) + 다른 id 행사 1건
            val displaySchedule = createTeamMemberSchedule(
                id = 1L,
                employeeId = userId,
                accountId = 8938,
                workingCategory1 = WorkingCategory1.EVENT,
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
                workingCategory1 = WorkingCategory1.EVENT,
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
                createTeamMemberSchedule(id = 1L, employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = "CLG001"),
                createTeamMemberSchedule(id = 2L, employeeId = userId, accountId = 8939, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = "CLG002"),
                createTeamMemberSchedule(id = 3L, employeeId = userId, accountId = 8940, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = null)
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

        @Test
        @DisplayName("조장 출근현황 집계 - 팀원 단위(distinct) + 진열 비대칭(안전점검 실시자만 분모) (레거시 home.jsp promcnt/sum 정합)")
        fun leaderAttendanceSummary_legacyAsymmetric() {
            // Given — 팀: 조장(1) + A(2,행사·출근완료) + B(3,행사·미출근) + C(4,진열·안전점검O) + D(5,진열·안전점검X)
            val userId = 1L
            val orgName = "부산1지점"
            val leader = createEmployee(id = userId, orgName = orgName, role = AppAuthority.LEADER)
            val a = createEmployee(id = 2L, employeeCode = "00000002", orgName = orgName, name = "A")
            val b = createEmployee(id = 3L, employeeCode = "00000003", orgName = orgName, name = "B")
            val c = createEmployee(id = 4L, employeeCode = "00000004", orgName = orgName, name = "C")
            val d = createEmployee(id = 5L, employeeCode = "00000005", orgName = orgName, name = "D")

            // 행사 TMS: A 출근완료, B 미출근
            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, employeeId = 2L, accountId = 8938, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = "CLG-A"),
                createTeamMemberSchedule(id = 2L, employeeId = 3L, accountId = 8939, workingCategory1 = WorkingCategory1.EVENT, commuteLogSfid = null)
            )
            // 확정 진열마스터: C, D
            val displayWorkSchedules = listOf(
                createDisplayWorkSchedule(id = 100L, employeeId = 4L, accountId = 8940),
                createDisplayWorkSchedule(id = 101L, employeeId = 5L, accountId = 8941)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(leader)
            every { employeeRepository.findByOrgName(orgName) } returns listOf(leader, a, b, c, d)
            every { teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(any(), any()) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns displayWorkSchedules
            // 안전점검 실시: C(4) 만
            every { safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(any(), any()) } returns
                listOf(SafetyCheckSubmission(employeeId = 4L, workingDate = LocalDate.now()))

            // When
            val result = homeService.getHomeData(userId)

            // Then
            // 분모 N: A(행사)·B(행사)·C(진열+안전점검) = 3. D(진열·안전점검X)는 제외 — 진열 비대칭
            assertThat(result.attendanceSummary.totalCount).isEqualTo(3)
            // 분자 M: 출근 등록 완료자 A = 1
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(1)
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
        @DisplayName("진열 TMS + 확정 마스터 동일 거래처 - 마스터로 1건, 출근여부는 진열 TMS 에서 읽음 (레거시 dtc2 조인 정합)")
        fun displaySchedule_fromMaster_commuteFromTms() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            val account = createAccount(id = 742, name = "테스트 거래처")

            // 진열 TMS (출근완료) — 일정 집계엔 쓰이지 않고 출근여부 조회용으로만 사용
            val teamMemberSchedule = createTeamMemberSchedule(
                id = 1L, employeeId = userId, accountId = 742,
                workingCategory1 = WorkingCategory1.DISPLAY, commuteLogSfid = "CLG001"
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

            // Then — 진열 TMS 단독은 일정으로 카운트되지 않고, 확정 마스터 1건만 반환
            assertThat(result.todaySchedules).hasSize(1)
            assertThat(result.todaySchedules[0].displayWorkScheduleId).isEqualTo(100L)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo(0L)
            // 출근여부는 매칭되는 진열 TMS 에서 읽는다
            assertThat(result.todaySchedules[0].isCommuteRegistered).isTrue()
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(1)
        }

        @Test
        @DisplayName("진열 TMS 만 존재(확정 마스터 없음) - 홈 일정 0건 (레거시 정합: 진열은 확정 마스터로만 집계, phantom 등록버튼 방지)")
        fun displayTmsWithoutMaster_notCounted() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, role = null)
            val account = createAccount(id = 742, name = "테스트 거래처")

            // 확정 진열마스터 backing 없는 진열 TMS 1건만 존재
            val displayTms = createTeamMemberSchedule(
                id = 1L, employeeId = userId, accountId = 742,
                workingCategory1 = WorkingCategory1.DISPLAY, commuteLogSfid = null
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, any()) } returns listOf(displayTms)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(any(), any()) } returns emptyList()
            every { accountRepository.findByIdIn(any()) } returns listOf(account)
            every { safetyCheckService.getTodayStatus(any()) } returns SafetyCheckTodayResponse(completed = false)
            every { noticeRepository.findRecentNotices(any()) } returns emptyList()

            // When
            val result = homeService.getHomeData(userId)

            // Then — 레거시는 진열 TMS 행 단독을 홈 일정으로 카운트하지 않음
            assertThat(result.todaySchedules).isEmpty()
            assertThat(result.attendanceSummary.totalCount).isEqualTo(0)
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(0)
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
        accountId: Long? = null,
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
            attendanceLog = commuteLogSfid?.let { AttendanceLog(id = 1L) },
            commuteReportDatetime = commuteReportDatetime
        )
    }

    private fun createDisplayWorkSchedule(
        id: Long = 0L,
        employeeId: Long? = null,
        accountId: Long? = null,
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
        id: Long = 0L,
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
