package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.config.AttendanceProperties
import com.otoki.powersales.domain.activity.schedule.exception.AccountCoordsMissingException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceDayOffConflictException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTimeExceededException
import com.otoki.powersales.domain.activity.schedule.exception.AlreadyRegisteredException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTargetConflictException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTargetRequiredException
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceDualBranchException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayAttendanceDuplicateException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotAssignedException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotConfirmedException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleOutOfRangeException
import com.otoki.powersales.domain.activity.schedule.exception.EventAttendanceDuplicateException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleDateMismatchException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleNotAssignedException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.DistanceExceededException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidCoordsException
import com.otoki.powersales.domain.activity.schedule.exception.SafetyCheckRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.TeamMemberScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.attendance.AttendanceRegistrar
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyIntegrationService
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.AttendanceService
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleOwnerResolver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.temporal.ChronoUnit

@DisplayName("AttendanceService 테스트")
class AttendanceServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator = mockk()
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository = mockk(relaxUnitFun = true)
    private val attendanceRegistrar: AttendanceRegistrar = mockk()
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService = mockk(relaxUnitFun = true)
    private val clock: Clock = mockk()
    private val attendanceProperties: AttendanceProperties = spyk(AttendanceProperties(gpsThresholdMeters = 500))
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver = mockk()

    private val attendanceService = AttendanceService(
        employeeRepository,
        teamMemberScheduleRepository,
        teamMemberScheduleNameGenerator,
        displayWorkScheduleRepository,
        safetyCheckSubmissionRepository,
        attendanceRegistrar,
        adminMonthlyIntegrationService,
        attendanceProperties,
        teamMemberScheduleOwnerResolver,
        clock,
    )

    init {
        every { teamMemberScheduleNameGenerator.next() } returns "TS00000001"
        every { teamMemberScheduleOwnerResolver.resolveOwner(any()) } returns null
    }

    @BeforeEach
    fun setUpClock() {
        // 기본: 오전 10시 (마감 전) — 기본 stub (개별 테스트에서 override 가능)
        val fixedClock = Clock.fixed(
            LocalDate.now().atTime(10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul")
        )
        every { clock.withZone(any()) } returns fixedClock
        // 부수 호출 — 개별 테스트가 override 가능 (MockK 는 마지막 stub 우선)
        every { teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any()) } returns false
        every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(any(), any(), any()) } returns 0L
    }

    // ========== getAccountList Tests ==========

    @Nested
    @DisplayName("getAccountList - 오늘 출근 거래처 목록 조회")
    inner class GetAccountListTests {

        @Test
        @DisplayName("오늘 스케줄 3건 + 안전점검 완료 - 전체 조회 -> 3건 반환, safetyCheckCompleted=true")
        fun getAccountList_threeSchedules_returnsThreeAccountsWithGps() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "이마트 강남점", accountLatitude = "37.4979", accountLongitude = "127.0276", accountAddress = "서울시 강남구"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "홈플러스 서초점", accountLatitude = "37.5000", accountLongitude = "127.0100", accountAddress = "서울시 서초구"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "롯데마트 송파점", accountLatitude = "37.5100", accountLongitude = "127.0500", accountAddress = "서울시 송파구")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.safetyCheckCompleted).isTrue()
            assertThat(result.accounts).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.currentDate).isEqualTo(today.toString())

            // GPS 좌표 포함 확인
            val account1 = result.accounts[0]
            assertThat(account1.scheduleId).isEqualTo(1L)
            assertThat(account1.accountName).isEqualTo("이마트 강남점")
            assertThat(account1.latitude).isEqualTo(37.4979)
            assertThat(account1.longitude).isEqualTo(127.0276)
            assertThat(account1.address).isEqualTo("서울시 강남구")
            assertThat(account1.isRegistered).isFalse()
        }

        @Test
        @DisplayName("안전점검 미완료 - safetyCheckCompleted=false, 거래처 목록은 정상 반환")
        fun getAccountList_safetyCheckNotCompleted_returnsFalse() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "이마트 강남점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns false
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.safetyCheckCompleted).isFalse()
            assertThat(result.accounts).hasSize(1)
        }

        @Test
        @DisplayName("workCategory3 매핑 - 스케줄의 workingCategory3가 '고정' -> 응답에 포함")
        fun getAccountList_workCategory3_mapped() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory3 = WorkingCategory3.FIXED,
                    accountName = "테스트 거래처A"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory3 = null,
                    accountName = "테스트 거래처B")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.accounts[0].workCategory3).isEqualTo("고정")
            assertThat(result.accounts[1].workCategory3).isNull()
        }

        @Test
        @DisplayName("키워드='이마트' - 3건 중 이마트 포함 결과만 -> 이마트 매장만 반환")
        fun getAccountList_withKeyword_returnsFilteredResults() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, accountName = "홈플러스 서초점"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940, accountName = "이마트 송파점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, "이마트")

            // Then
            assertThat(result.accounts).hasSize(2)
            assertThat(result.accounts.all { it.accountName.contains("이마트") }).isTrue()
            assertThat(result.totalCount).isEqualTo(2)
        }

        @Test
        @DisplayName("키워드='강남' - 주소에 '강남' 포함 거래처 -> 주소 매칭 결과 반환")
        fun getAccountList_withAddressKeyword_returnsFilteredResults() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "이마트 강남점", accountAddress = "서울시 강남구 역삼동"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, accountName = "홈플러스 서초점", accountAddress = "서울시 서초구 반포동"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940, accountName = "롯데마트 송파점", accountAddress = "서울시 송파구 잠실동")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, "강남")

            // Then
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountName).isEqualTo("이마트 강남점")
            assertThat(result.accounts[0].address).isEqualTo("서울시 강남구 역삼동")
        }

        @Test
        @DisplayName("키워드='2001' - 거래처코드 매칭 -> 해당 거래처만 반환")
        fun getAccountList_withAccountTypeCodeKeyword_returnsFilteredResults() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "이마트 강남점", accountAbcTypeCode = "2001"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, accountName = "홈플러스 서초점", accountAbcTypeCode = "3001")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, "2001")

            // Then
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountName).isEqualTo("이마트 강남점")
            assertThat(result.accounts[0].accountTypeCode).isEqualTo("2001")
        }

        @Test
        @DisplayName("키워드='서울' - 거래처명+주소 복합 매칭 -> 중복 없이 모두 반환")
        fun getAccountList_withKeywordMatchingMultipleFields_returnsAllMatches() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "서울마트", accountAddress = "경기도 수원시"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, accountName = "홈플러스 부산점", accountAddress = "서울시 강남구"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940, accountName = "롯데마트 대전점", accountAddress = "대전시 유성구")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, "서울")

            // Then
            assertThat(result.accounts).hasSize(2)
            assertThat(result.accounts.map { it.accountName }).containsExactlyInAnyOrder("서울마트", "홈플러스 부산점")
        }

        @Test
        @DisplayName("키워드='1234' + accountTypeCode null -> NPE 미발생, 해당 거래처 제외")
        fun getAccountList_withNullAccountTypeCode_noNpe() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, accountName = "이마트", accountAddress = "서울시", accountAbcTypeCode = null),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, accountName = "홈플러스", accountAddress = "부산시", accountAbcTypeCode = "1234")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, "1234")

            // Then
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountName).isEqualTo("홈플러스")
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException 발생")
        fun getAccountList_userNotFound_throwsException() {
            // Given
            val userId = 999L
            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { attendanceService.getAccountList(userId, null) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("오늘 스케줄 없음 -> 빈 목록 반환")
        fun getAccountList_noSchedules_returnsEmptyList() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns false
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.accounts).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일부 출근 등록 완료(commuteLogSfid 존재) -> registeredCount 반영")
        fun getAccountList_withPartialRegistrations_returnsCorrectCount() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogSfid = "OK", accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogSfid = null, accountName = "홈플러스 서초점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.accounts[0].isRegistered).isTrue()
            assertThat(result.accounts[1].isRegistered).isFalse()
        }
    }

    // ========== getAccountList - 진열마스터 병합 Tests ==========

    @Nested
    @DisplayName("getAccountList - 진열마스터 + 기존 일정 병합")
    inner class GetAccountListMasterMergeTests {

        @Test
        @DisplayName("여사원일정 1건 + 진열마스터 2건(확정, 오늘 유효) -> 총 3건 (source=schedule 1건, source=master 2건)")
        fun getAccountList_mergeScheduleAndMasters_returnsAll() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938,
                    accountName = "이마트 강남점", workingCategory1 = WorkingCategory1.DISPLAY)
            )

            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9001, accountName = "롯데마트 송파점"),
                createDisplayWorkSchedule(id = 101L, confirmed = true,
                    startDate = today.minus(5, ChronoUnit.DAYS), endDate = today.plus(5, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9002, accountName = "홈플러스 서초점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.accounts).hasSize(3)
            val scheduleSources = result.accounts.filter { it.source == "schedule" }
            val masterSources = result.accounts.filter { it.source == "master" }
            assertThat(scheduleSources).hasSize(1)
            assertThat(masterSources).hasSize(2)
        }

        @Test
        @DisplayName("진열마스터 거래처가 이미 여사원일정에 존재 -> 중복 제거, 1건만 (source=schedule)")
        fun getAccountList_duplicateAccountInMasterAndSchedule_deduplicates() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            // 여사원일정에 accountId=8938 존재
            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938,
                    accountName = "이마트 강남점", workingCategory1 = WorkingCategory1.DISPLAY)
            )

            // 진열마스터에도 동일 accountId=8938 존재
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 8938, accountName = "이마트 강남점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then - 중복 제거로 1건만
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].source).isEqualTo("schedule")
            assertThat(result.accounts[0].scheduleId).isEqualTo(1L)
        }

        @Test
        @DisplayName("같은 거래처에 행사(EVENT) TMS + 진열마스터 동시 -> 둘 다 표시 (행사 schedule 1건 + 진열 master 1건). 홈 화면과 일치")
        fun getAccountList_eventScheduleAndDisplayMaster_sameAccount_returnsBoth() {
            // Given - 같은 accountId=8938 에 TMS 는 행사(EVENT) 만, 진열은 마스터에만 존재
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            // 여사원일정에 accountId=8938 행사(EVENT) 존재
            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938,
                    accountName = "이마트 원주점", workingCategory1 = WorkingCategory1.EVENT)
            )

            // 진열마스터에도 동일 accountId=8938 존재
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 8938, accountName = "이마트 원주점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then - 행사(schedule) + 진열(master) 둘 다 노출 (dedup 키는 진열로 잡힌 거래처로 한정)
            assertThat(result.accounts).hasSize(2)
            val eventItem = result.accounts.single { it.source == "schedule" }
            val displayItem = result.accounts.single { it.source == "master" }
            assertThat(eventItem.workCategory).isEqualTo("행사")
            assertThat(displayItem.workCategory).isEqualTo("진열")
            assertThat(displayItem.accountId).isEqualTo(8938)
        }

        @Test
        @DisplayName("마감 전(16:59) - isRegistrationClosed=false, registrationDeadline='17:00'")
        fun getAccountList_beforeDeadline_registrationOpen() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val beforeDeadlineClock = Clock.fixed(
                today.atTime(16, 59).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns beforeDeadlineClock

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.registrationDeadline).isEqualTo("17:00")
            assertThat(result.isRegistrationClosed).isFalse()
        }

        @Test
        @DisplayName("마감 후(17:00) - isRegistrationClosed=true, registrationDeadline='17:00'")
        fun getAccountList_afterDeadline_registrationClosed() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val afterDeadlineClock = Clock.fixed(
                today.atTime(17, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns afterDeadlineClock

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.registrationDeadline).isEqualTo("17:00")
            assertThat(result.isRegistrationClosed).isTrue()
        }

        @Test
        @DisplayName("마감 시각 override(21:00) - 17시 이후에도 마감 전, registrationDeadline='21:00'")
        fun getAccountList_deadlineOverride_registrationOpenAfter17() {
            // Given — dev 환경처럼 registrationDeadline 을 21:00 으로 override
            every { attendanceProperties.registrationDeadline } returns LocalTime.of(21, 0)

            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            // 운영 마감(17:00) 은 지났지만 override 마감(21:00) 이전인 20:00
            val clockAt20 = Clock.fixed(
                today.atTime(20, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns clockAt20

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.registrationDeadline).isEqualTo("21:00")
            assertThat(result.isRegistrationClosed).isFalse()
        }
    }

    // ========== getAccountList - 임시 근무 우선순위 정렬 Tests ==========

    @Nested
    @DisplayName("getAccountList - 임시 근무 우선순위 정렬")
    inner class GetAccountListTempWorkerPriorityTests {

        @Test
        @DisplayName("임시+일반 혼합 정렬 - master 임시(미등록) 1건 + schedule 일반(미등록) 2건 -> 임시(master) 가 먼저")
        fun getAccountList_tempAndNormal_tempFirst() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 9001,
                    accountName = "일반거래처A", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DEDICATED),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 9003,
                    accountName = "일반거래처B", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DEDICATED)
            )
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9002, accountName = "임시거래처", typeOfWork5 = TypeOfWork5.TEMPORARY)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts[0].accountName).isEqualTo("임시거래처")
            assertThat(result.accounts[0].workCategory2).isEqualTo("임시")
        }

        @Test
        @DisplayName("등록완료+임시(master)+일반 정렬 - 등록완료 → 임시(master) → 일반(schedule) 순서")
        fun getAccountList_registeredTempNormal_correctOrder() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 9001,
                    accountName = "일반거래처", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DEDICATED),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 9002,
                    accountName = "등록완료거래처", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DEDICATED,
                    commuteLogSfid = "LOG001")
            )
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9003, accountName = "임시거래처", typeOfWork5 = TypeOfWork5.TEMPORARY)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then - 등록완료 → 임시(master) → 일반(schedule)
            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts[0].accountName).isEqualTo("등록완료거래처")
            assertThat(result.accounts[0].isRegistered).isTrue()
            assertThat(result.accounts[1].accountName).isEqualTo("임시거래처")
            assertThat(result.accounts[1].workCategory2).isEqualTo("임시")
            assertThat(result.accounts[2].accountName).isEqualTo("일반거래처")
            assertThat(result.accounts[2].workCategory2).isEqualTo("전담")
        }

        @Test
        @DisplayName("임시 없음 (기존 동작 유지) - 일반(미등록) 3건 -> source 기준 정렬 유지")
        fun getAccountList_noTemp_existingSortMaintained() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 9001,
                    accountName = "스케줄거래처", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DEDICATED)
            )
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9002, accountName = "마스터거래처A", typeOfWork5 = TypeOfWork5.REGULAR),
                createDisplayWorkSchedule(id = 101L, confirmed = true,
                    startDate = today.minus(5, ChronoUnit.DAYS), endDate = today.plus(5, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9003, accountName = "마스터거래처B", typeOfWork5 = TypeOfWork5.REGULAR)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then - source=schedule이 master보다 먼저
            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts[0].source).isEqualTo("schedule")
            assertThat(result.accounts[1].source).isEqualTo("master")
            assertThat(result.accounts[2].source).isEqualTo("master")
        }

        @Test
        @DisplayName("workCategory2 응답 확인 - schedule 소스는 workingCategory2, master 소스는 typeOfWork5")
        fun getAccountList_workCategory2_fromCorrectSource() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 9001,
                    accountName = "스케줄거래처", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = WorkingCategory2.DISPLAY_CONCURRENT)
            )
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9002, accountName = "마스터거래처", typeOfWork5 = TypeOfWork5.TEMPORARY)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            val scheduleAccount = result.accounts.first { it.source == "schedule" }
            val masterAccount = result.accounts.first { it.source == "master" }
            assertThat(scheduleAccount.workCategory2).isEqualTo("진열겸임")
            assertThat(masterAccount.workCategory2).isEqualTo("임시")
        }

        @Test
        @DisplayName("workCategory2 null (schedule) + 임시(master) 혼합 - master 임시가 먼저, null 은 일반으로 취급")
        fun getAccountList_nullWorkCategory2_treatedAsNormal() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 9001,
                    accountName = "null카테고리거래처", workingCategory1 = WorkingCategory1.DISPLAY, workingCategory2 = null)
            )
            val masters = listOf(
                createDisplayWorkSchedule(id = 100L, confirmed = true,
                    startDate = today.minus(10, ChronoUnit.DAYS), endDate = today.plus(10, ChronoUnit.DAYS),
                    employeeId = userId, accountId = 9002, accountName = "임시거래처", typeOfWork5 = TypeOfWork5.TEMPORARY)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, today) } returns masters

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then - 임시(master) 가 먼저, null(schedule) 은 일반으로 취급
            assertThat(result.accounts).hasSize(2)
            assertThat(result.accounts[0].accountName).isEqualTo("임시거래처")
            assertThat(result.accounts[0].workCategory2).isEqualTo("임시")
            assertThat(result.accounts[1].accountName).isEqualTo("null카테고리거래처")
            assertThat(result.accounts[1].workCategory2).isNull()
        }
    }

    // ========== register Tests ==========

    @Nested
    @DisplayName("register - 출근 등록")
    inner class RegisterTests {

        // 강남역 기준 좌표
        private val accountLat = 37.4979
        private val accountLon = 127.0276

        // 약 0.3km 거리 (0.277km)
        private val nearUserLat = 37.4995
        private val nearUserLon = 127.0300

        // 약 1.2km 거리 (1.212km)
        private val farUserLat = 37.5088
        private val farUserLon = 127.0276

        @Test
        @DisplayName("안전점검 완료 + 거리 범위 내(0.3km, 허용 0.5km) - 출근 등록 -> 성공")
        fun register_withinDistance_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.accountName).isEqualTo("이마트 강남점")
            // Spec #585 Q4: 응답에 실제 거리 미노출 → 항상 0.0
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.workType).isEqualTo("근무")
        }

        @Test
        @DisplayName("안전점검 미완료 - 출근 등록 시도 -> SafetyCheckRequiredException")
        fun register_safetyCheckNotCompleted_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns false

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(SafetyCheckRequiredException::class.java)
        }

        @Test
        @DisplayName("거리 초과(1.2km, 허용 0.5km) - 비면제 코드 2110 -> DistanceExceededException")
        fun register_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("대리점 면제 코드 1110 - 5km 거리 -> 거리 검증 생략, 성공")
        fun register_exemptCode1110_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "대리점A", accountAbcTypeCode = "1110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // 5km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val farLat = 37.5429  // ~5km north
            val farLon = 127.0276

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, farLat, farLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("대리점A")
            // Spec #586: 면제 코드 적용 시 gpsSkipped=true, gpsSkipReason=ABC_EXEMPT
            assertThat(result.gpsSkipped).isTrue()
            assertThat(result.gpsSkipReason).isEqualTo("ABC_EXEMPT")
        }

        @Test
        @DisplayName("대리점 면제 코드 1900 - 10km 거리 -> 거리 검증 생략, 성공")
        fun register_exemptCode1900_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "특수거래처B", accountAbcTypeCode = "1900",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // 10km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val veryFarLat = 37.5879  // ~10km north
            val veryFarLon = 127.0276

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, veryFarLat, veryFarLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("특수거래처B")
            // Spec #586: 면제 코드 적용 시 gpsSkipped=true, gpsSkipReason=ABC_EXEMPT
            assertThat(result.gpsSkipped).isTrue()
            assertThat(result.gpsSkipReason).isEqualTo("ABC_EXEMPT")
        }

        @Test
        @DisplayName("Spec #586 §8-H5 — 면제 거래처 + 거래처 좌표 누락(null) → 등록 성공, gpsSkipped=true (좌표 누락 검증 미진입)")
        fun register_exemptCodeWithNullAccountCoords_success() {
            // Given — 면제 코드 1110 + 거래처 latitude/longitude 모두 null
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "대리점A", accountAbcTypeCode = "1110",
                accountLatitude = null, accountLongitude = null
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When — 좌표 누락이지만 면제 코드이므로 ATT_ACCOUNT_COORDS_MISSING 미발생
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.gpsSkipped).isTrue()
            assertThat(result.gpsSkipReason).isEqualTo("ABC_EXEMPT")
        }

        @Test
        @DisplayName("Spec #586 §8-H6 — 면제 거래처 + 거래처 좌표 파싱 불가 → 등록 성공")
        fun register_exemptCodeWithUnparseableAccountCoords_success() {
            // Given — 면제 코드 1900 + 거래처 latitude='invalid'
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "특수거래처B", accountAbcTypeCode = "1900",
                accountLatitude = "invalid", accountLongitude = "invalid"
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.gpsSkipped).isTrue()
            assertThat(result.gpsSkipReason).isEqualTo("ABC_EXEMPT")
        }

        @Test
        @DisplayName("Spec #586 §8-H7 — 비면제 거래처 + 거리 통과 → gpsSkipped=false, gpsSkipReason=null")
        fun register_nonExemptWithinDistance_gpsSkippedFalse() {
            // Given (비면제 코드 + 거리 ~277m, threshold=500m → 통과)
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "9999",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.gpsSkipped).isFalse()
            assertThat(result.gpsSkipReason).isNull()
        }

        @Test
        @DisplayName("Spec #586 §8-H9 — 비면제 거래처 + 좌표 누락 → ATT_ACCOUNT_COORDS_MISSING (면제 정책 미적용)")
        fun register_nonExemptWithMissingCoords_throwsAccountCoordsMissing() {
            // Given (비면제 코드 9999 + latitude=null)
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "일반매장", accountAbcTypeCode = "9999",
                accountLatitude = null, accountLongitude = null
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AccountCoordsMissingException::class.java)
        }

        @Test
        @DisplayName("비면제 코드 2110 - 1.2km 거리 -> DistanceExceededException")
        fun register_nonExemptCode2110_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "일반매장", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("중복 출근 등록(commuteLogSfid='OK') -> AlreadyRegisteredException")
        fun register_alreadyRegistered_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = "OK"
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AlreadyRegisteredException::class.java)
        }

        @Test
        @DisplayName("스케줄 미존재 -> TeamMemberScheduleNotFoundException")
        fun register_scheduleNotFound_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 99999L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(TeamMemberScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException")
        fun register_userNotFound_throwsException() {
            // Given
            val userId = 999L
            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("대휴 날짜 출근 등록 시도 -> AttendanceDayOffConflictException")
        fun register_substituteHolidayConflict_throwsException() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(employee, today, WorkingType.ALT_HOLIDAY) } returns true

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceDayOffConflictException::class.java)

            // 안전점검 검증까지 도달하지 않아야 한다
            verify(exactly = 0) { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(any(), any()) }
        }

        @Test
        @DisplayName("일반 근무일 출근 등록 - 대휴 검증 통과 후 기존 플로우 진행")
        fun register_normalDay_passesDayOffCheck() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(employee, today, WorkingType.ALT_HOLIDAY) } returns false
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
        }

        @Test
        @DisplayName("workType 파라미터 전달 - workType 우선 반환")
        fun register_withWorkType_returnsProvidedWorkType() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, "냉장")

            // Then
            assertThat(result.workType).isEqualTo("냉장")
        }

        @Test
        @DisplayName("출근 등록 성공 시 totalCount, registeredCount 정확 반환")
        fun register_success_returnsCorrectCounts() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val targetTeamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            // 오늘 전체 스케줄 3건 (1건 이미 등록, 1건 지금 등록, 1건 미등록)
            val allTeamMemberSchedules = listOf(
                targetTeamMemberSchedule,
                createTeamMemberSchedule(id = 20L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogSfid = "OK"),
                createTeamMemberSchedule(id = 30L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogSfid = null)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(targetTeamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns allTeamMemberSchedules

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            // id=20 has commuteLogSfid="OK", id=10 matches scheduleId => 2 registered
            assertThat(result.registeredCount).isEqualTo(2)
        }

        @Test
        @DisplayName("17시 이후(17:00) - 출근등록 시도 -> AttendanceTimeExceededException")
        fun register_afterDeadline_throwsException() {
            // Given
            val afterDeadlineClock = Clock.fixed(
                LocalDate.now().atTime(17, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns afterDeadlineClock

            // When & Then
            assertThatThrownBy {
                attendanceService.register(1L, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceTimeExceededException::class.java)
        }

        @Test
        @DisplayName("23시 59분 - 출근등록 시도 -> AttendanceTimeExceededException")
        fun register_lateNight_throwsException() {
            // Given
            val lateNightClock = Clock.fixed(
                LocalDate.now().atTime(23, 59).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns lateNightClock

            // When & Then
            assertThatThrownBy {
                attendanceService.register(1L, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceTimeExceededException::class.java)
        }

        @Test
        @DisplayName("16시 59분 - 출근등록 시도 -> 시간 검증 통과 (후속 검증으로 진행)")
        fun register_beforeDeadline_passesTimeCheck() {
            // Given
            val beforeDeadlineClock = Clock.fixed(
                LocalDate.now().atTime(16, 59).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns beforeDeadlineClock

            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns false

            // When & Then — 시간은 통과하고 안전점검 예외 발생 (시간 이후 로직까지 도달 확인)
            assertThatThrownBy {
                attendanceService.register(userId, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(SafetyCheckRequiredException::class.java)
        }

        @Test
        @DisplayName("마감 시각 override(21:00) - 20시 출근등록 시도 -> 시간 검증 통과 (dev 정합)")
        fun register_deadlineOverride_passesTimeCheckAfter17() {
            // Given — dev 환경처럼 registrationDeadline 을 21:00 으로 override
            every { attendanceProperties.registrationDeadline } returns LocalTime.of(21, 0)

            // 운영 마감(17:00) 은 지났지만 override 마감(21:00) 이전인 20:00
            val clockAt20 = Clock.fixed(
                LocalDate.now().atTime(20, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            every { clock.withZone(any()) } returns clockAt20

            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns false

            // When & Then — 시간은 통과하고 안전점검 예외 발생 (시간 이후 로직까지 도달 확인)
            assertThatThrownBy {
                attendanceService.register(userId, 10L, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(SafetyCheckRequiredException::class.java)
        }

        // ========== Spec #585 — GPS Haversine 거리 검증 (m 기준) ==========

        @Test
        @DisplayName("Spec #585 §7-#3 — 임계값 1m 초과(distance ~1212m, threshold=500) -> ATT_GPS_DISTANCE_EXCEEDED")
        fun register_overThreshold_throwsAttGpsDistanceExceeded() {
            // Given (default threshold = 500m, far ~1.2km)
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            val ex = assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
            // 에러코드 검증
            assertThat(DistanceExceededException().errorCode).isEqualTo("ATT_GPS_DISTANCE_EXCEEDED")
        }

        @Test
        @DisplayName("Spec #585 §7-#2 — 임계값(=277m) 정확 일치 시 등록 통과 (> 비교)")
        fun register_exactlyAtThreshold_passes() {
            // Given (계산 거리 ~277m, threshold = 277m → distanceMeters > thresholdMeters 거짓 → 통과)
            every { attendanceProperties.gpsThresholdMeters } returns 277

            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
        }

        @Test
        @DisplayName("Spec #585 §7-#4 — 거래처 latitude=null -> ATT_ACCOUNT_COORDS_MISSING")
        fun register_accountLatitudeNull_throwsAccountCoordsMissing() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = null, accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AccountCoordsMissingException::class.java)
        }

        @Test
        @DisplayName("Spec #585 §7-#5 — 거래처 latitude=공백 -> ATT_ACCOUNT_COORDS_MISSING")
        fun register_accountLatitudeBlank_throwsAccountCoordsMissing() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = "   ", accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AccountCoordsMissingException::class.java)
        }

        @Test
        @DisplayName("Spec #585 §7-#6 — 거래처 latitude='abc'(파싱 실패) -> ATT_ACCOUNT_COORDS_MISSING")
        fun register_accountLatitudeNotNumeric_throwsAccountCoordsMissing() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = "abc", accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AccountCoordsMissingException::class.java)
        }

        @Test
        @DisplayName("Spec #585 §7-#7 — 거래처 latitude='91.0'(범위 초과) -> ATT_ACCOUNT_COORDS_MISSING")
        fun register_accountLatitudeOutOfRange_throwsAccountCoordsMissing() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = "91.0", accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AccountCoordsMissingException::class.java)
        }

        @Test
        @DisplayName("Spec #585 §7-#8 — 사원 currentLat=91.0(범위 초과) -> ATT_INVALID_COORDS")
        fun register_currentLatitudeOutOfRange_throwsInvalidCoords() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, null, null, 91.0, nearUserLon, null)
            }.isInstanceOf(InvalidCoordsException::class.java)
        }

        @Test
        @DisplayName("Spec #585 — 임계값 환경 override(1000m)로 ~1.2km 거리 통과 가능")
        fun register_thresholdOverride_passesAtFarDistance() {
            // Given — threshold 를 1500m 로 override 하면 ~1212m 거리는 통과
            every { attendanceProperties.gpsThresholdMeters } returns 1500

            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, commuteLogSfid = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, farUserLat, farUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
        }
    }

    // ========== register - 진열마스터 기반 출근 등록 Tests ==========

    @Nested
    @DisplayName("register - 진열마스터 기반 출근 등록")
    inner class RegisterByDisplayWorkScheduleTests {

        private val accountLat = 37.4979
        private val accountLon = 127.0276
        private val nearUserLat = 37.4995
        private val nearUserLon = 127.0300

        @Test
        @DisplayName("정상 - 신규 생성 + 출근: valid displayWorkScheduleId, 기존 TMS 없음 -> TMS 생성, 출근등록, refreshIntegration 호출")
        fun register_byDisplayWorkSchedule_newCreation_success() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val account = Account(
                id = 8938,
                name = "테스트 거래처",
                address1 = "서울시 강남구",
                abcTypeCode = "2110",
                latitude = accountLat.toString(),
                longitude = accountLon.toString()
            )

            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(30, ChronoUnit.DAYS),
                endDate = today.plus(30, ChronoUnit.DAYS),
                typeOfWork3 = TypeOfWork3.FIXED,
                typeOfWork5 = TypeOfWork5.REGULAR,
                employeeId = userId,
                accountId = 8938,
                accountName = "테스트 거래처",
                accountLatitude = accountLat.toString(),
                accountLongitude = accountLon.toString(),
                accountAbcTypeCode = "2110"
            )

            val teamLeader = createEmployee(id = 99L, sfid = "LEADER001", name = "조장", role = AppAuthority.LEADER)

            // 신규 생성될 TMS
            val newTms = createTeamMemberSchedule(
                id = 50L, sfid = null, employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, commuteLogSfid = null,
                accountName = "테스트 거래처", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            every { employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC001"), AppAuthority.LEADER) } returns listOf(teamLeader)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } answers {
                firstArg<TeamMemberSchedule>().also { _ ->
                    // simulate saved entity with id
                }
            } andThen newTms
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(newTms)

            // When
            val result = attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.accountName).isEqualTo("테스트 거래처")
            verify { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
            verify { adminMonthlyIntegrationService.refreshIntegration(
                employeeId = userId,
                yearMonth = YearMonth.from(today)
            ) }
        }

        @Test
        @DisplayName("정상 - 기존 여사원일정 재사용: 동일 사원+거래처+오늘 TMS 존재 -> 기존 사용, 출근 등록 후 refreshIntegration 호출 (SF 레거시 동등)")
        fun register_byDisplayWorkSchedule_existingTms_reused() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val account = Account(
                id = 8938,
                name = "테스트 거래처",
                address1 = "서울시 강남구",
                abcTypeCode = "2110",
                latitude = accountLat.toString(),
                longitude = accountLon.toString()
            )

            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(30, ChronoUnit.DAYS),
                endDate = today.plus(30, ChronoUnit.DAYS),
                employeeId = userId,
                accountId = 8938,
                accountName = "테스트 거래처",
                accountLatitude = accountLat.toString(),
                accountLongitude = accountLon.toString(),
                accountAbcTypeCode = "2110"
            )

            // 기존 TMS가 이미 존재
            val existingTms = createTeamMemberSchedule(
                id = 50L, sfid = "SCH050", employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, commuteLogSfid = null,
                accountName = "테스트 거래처", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns existingTms
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(existingTms)

            // When
            val result = attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(50L)
            verify(exactly = 0) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
            // SF 레거시 동등: 기존 일정에 출근만 찍어도 환산 일정 재집계 (CommuteLogId__c null→not null = beforeUpdate 트리거)
            verify { adminMonthlyIntegrationService.refreshIntegration(userId, YearMonth.from(today)) }
        }

        @Test
        @DisplayName("마스터 미존재 -> DisplayScheduleNotFoundException")
        fun register_byDisplayWorkSchedule_notFound_throwsException() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 999999L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("마스터 미확정(confirmed=false) -> DisplayScheduleNotConfirmedException")
        fun register_byDisplayWorkSchedule_notConfirmed_throwsException() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")

            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = false,
                startDate = today.minus(30, ChronoUnit.DAYS),
                endDate = today.plus(30, ChronoUnit.DAYS)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayScheduleNotConfirmedException::class.java)
        }

        @Test
        @DisplayName("기간 범위 밖 (오늘 < startDate) -> DisplayScheduleOutOfRangeException")
        fun register_byDisplayWorkSchedule_outOfRange_throwsException() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")

            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.plus(10, ChronoUnit.DAYS),  // 미래 시작일
                endDate = today.plus(40, ChronoUnit.DAYS)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayScheduleOutOfRangeException::class.java)
        }

        @Test
        @DisplayName("scheduleId, displayWorkScheduleId 둘 다 null -> AttendanceTargetRequiredException")
        fun register_bothNull_throwsException() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, null, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceTargetRequiredException::class.java)
        }

        @Test
        @DisplayName("scheduleId, displayWorkScheduleId 둘 다 값 -> AttendanceTargetConflictException")
        fun register_bothProvided_throwsException() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, 10L, 100L, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceTargetConflictException::class.java)
        }

        // ========== Spec #587 P1-B 신규 시나리오 ==========

        @Test
        @DisplayName("Spec #587 T1 — 진열 출근 정상: 응답에 attendanceType=DISPLAY + 마스터 메타(start/end) 포함")
        fun register_byDisplayWorkSchedule_responsePayload_containsDisplayMetadata() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()
            val startDate = today.minus(10, ChronoUnit.DAYS)
            val endDate = today.plus(20, ChronoUnit.DAYS)

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = startDate,
                endDate = endDate,
                typeOfWork3 = TypeOfWork3.FIXED,
                typeOfWork5 = TypeOfWork5.REGULAR,
                employeeId = userId,
                accountId = 8938,
                accountName = "테스트 거래처",
                accountLatitude = accountLat.toString(),
                accountLongitude = accountLon.toString(),
                accountAbcTypeCode = "2110"
            )
            val newTms = createTeamMemberSchedule(
                id = 50L, sfid = null, employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, workingCategory3 = WorkingCategory3.FIXED,
                accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), any()) } returns 0L
            every { employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC001"), AppAuthority.LEADER) } returns emptyList()
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns newTms
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(newTms)

            // When
            val result = attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.attendanceType).isEqualTo(AttendanceType.DISPLAY)
            assertThat(result.displayWorkScheduleId).isEqualTo(displayWorkScheduleId)
            assertThat(result.scheduleStartDate).isEqualTo(startDate)
            assertThat(result.scheduleEndDate).isEqualTo(endDate)
        }

        @Test
        @DisplayName("Spec #587 T3 — 본인 할당 안 됨: 마스터의 employee.id != currentEmployeeId -> DisplayScheduleNotAssignedException")
        fun register_byDisplayWorkSchedule_notAssignedToCurrentUser_throwsException() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(10, ChronoUnit.DAYS),
                endDate = today.plus(10, ChronoUnit.DAYS),
                employeeId = otherUserId, // 타인 할당
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayScheduleNotAssignedException::class.java)

            verify(exactly = 0) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("Spec #587 T7 — 고정 등록 + 기존 고정 1건 존재 (다른 거래처) -> DisplayAttendanceDuplicateException")
        fun register_byDisplayWorkSchedule_duplicateWorkingCategory3_throwsException() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(10, ChronoUnit.DAYS),
                endDate = today.plus(10, ChronoUnit.DAYS),
                typeOfWork3 = TypeOfWork3.FIXED,
                employeeId = userId,
                accountId = 8938,
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            // 동일 사원+거래처+오늘은 없음 → step 4-1 통과
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            // 다른 거래처에 고정 1건 존재 → 매트릭스상 고정 등록은 기존 고정 ≥ 1 이면 거부
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.FIXED)) } returns 1L

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayAttendanceDuplicateException::class.java)

            verify(exactly = 0) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("레거시 매트릭스 — 순회 등록 + 기존 순회 1건만 존재: 허용 (출근 미등록 사전배정 순회 일정이 막지 않음)")
        fun register_byDisplayWorkSchedule_patrolWithExistingPatrolOnly_allows() {
            // Given — 운영 버그 재현 시나리오: 출근 안 찍힌 순회 일정 1건이 사전 배정되어 있어도 진열(순회) 출근은 허용되어야 한다.
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(10, ChronoUnit.DAYS),
                endDate = today.plus(10, ChronoUnit.DAYS),
                typeOfWork3 = TypeOfWork3.ROTATION,
                typeOfWork5 = TypeOfWork5.REGULAR,
                employeeId = userId,
                accountId = 8938,
                accountLatitude = accountLat.toString(),
                accountLongitude = accountLon.toString(),
                accountAbcTypeCode = "2110",
            )
            val newTms = createTeamMemberSchedule(
                id = 51L, sfid = null, employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, workingCategory3 = WorkingCategory3.PATROL,
                accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            // 기존: 순회 1건만 존재 (고정 0, 격고 0) → 순회 등록은 고정 ≥ 1 또는 격고 ≥ 2 일 때만 거부 → 허용
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.FIXED)) } returns 0L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.ALTERNATE)) } returns 0L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.PATROL)) } returns 1L
            every { employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC001"), AppAuthority.LEADER) } returns emptyList()
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns newTms
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(newTms)

            // When
            val result = attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)

            // Then — 거부 없이 출근 등록 성공
            assertThat(result.attendanceType).isEqualTo(AttendanceType.DISPLAY)
            verify(exactly = 1) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("레거시 매트릭스 — 격고 등록 + 기존 격고 1건만 존재: 허용 (격고는 최대 2건까지)")
        fun register_byDisplayWorkSchedule_alternateWithOneExistingAlternate_allows() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(10, ChronoUnit.DAYS),
                endDate = today.plus(10, ChronoUnit.DAYS),
                typeOfWork3 = TypeOfWork3.GAP,
                typeOfWork5 = TypeOfWork5.REGULAR,
                employeeId = userId,
                accountId = 8938,
                accountLatitude = accountLat.toString(),
                accountLongitude = accountLon.toString(),
                accountAbcTypeCode = "2110",
            )
            val newTms = createTeamMemberSchedule(
                id = 52L, sfid = null, employeeId = userId, accountId = 8938,
                workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, workingCategory3 = WorkingCategory3.ALTERNATE,
                accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            // 기존: 격고 1건 (고정 0, 순회 0) → 격고 등록은 격고 ≥ 2 일 때만 거부 → 허용
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.FIXED)) } returns 0L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.ALTERNATE)) } returns 1L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.PATROL)) } returns 0L
            every { employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC001"), AppAuthority.LEADER) } returns emptyList()
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } returns newTms
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(newTms)

            // When
            val result = attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)

            // Then — 격고 2건째이므로 허용
            assertThat(result.attendanceType).isEqualTo(AttendanceType.DISPLAY)
            verify(exactly = 1) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("레거시 매트릭스 — 격고 등록 + 기존 격고 2건 존재: DisplayAttendanceDuplicateException (상한 초과)")
        fun register_byDisplayWorkSchedule_alternateWithTwoExistingAlternate_throws() {
            // Given
            val userId = 1L
            val displayWorkScheduleId = 100L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001", costCenterCode = "CC001")
            val master = createDisplayWorkSchedule(
                id = displayWorkScheduleId,
                confirmed = true,
                startDate = today.minus(10, ChronoUnit.DAYS),
                endDate = today.plus(10, ChronoUnit.DAYS),
                typeOfWork3 = TypeOfWork3.GAP,
                employeeId = userId,
                accountId = 8938,
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { displayWorkScheduleRepository.findById(displayWorkScheduleId) } returns Optional.of(master)
            every { teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(eq(employee), any(), eq(today)) } returns null
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.FIXED)) } returns 0L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.ALTERNATE)) } returns 2L
            every { teamMemberScheduleRepository.countByEmployeeAndWorkingDateAndWorkingCategory3(eq(employee), eq(today), eq(WorkingCategory3.PATROL)) } returns 0L

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, displayWorkScheduleId, null, nearUserLat, nearUserLon, null)
            }.isInstanceOf(DisplayAttendanceDuplicateException::class.java)

            verify(exactly = 0) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("Spec #587 T9/T12 — scheduleId 일반 경로 응답: attendanceType=REGULAR + display 메타 미포함")
        fun register_byScheduleId_responsePayload_isRegular() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")

            val tms = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH010", employeeId = userId, accountId = 8938,
                accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(tms)
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(tms)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.attendanceType).isEqualTo(AttendanceType.REGULAR)
            assertThat(result.displayWorkScheduleId).isNull()
            assertThat(result.scheduleStartDate).isNull()
            assertThat(result.scheduleEndDate).isNull()
        }
    }

    // ========== register - 행사 일정 기반 (Spec #587 P2-B) Tests ==========

    @Nested
    @DisplayName("register - 행사 일정 기반 출근 등록 (Spec #587 P2-B)")
    inner class RegisterByEventScheduleTests {

        private val accountLat = 37.4979
        private val accountLon = 127.0276
        private val nearUserLat = 37.4995
        private val nearUserLon = 127.0300

        @Test
        @DisplayName("Spec #587 P2-B T1 — 행사 출근 정상: 응답에 attendanceType=EVENT + 행사 메타 포함")
        fun register_byEventSchedule_success() {
            // Given
            val userId = 1L
            val eventScheduleId = 789L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001")
            val tms = createTeamMemberSchedule(
                id = eventScheduleId, sfid = "SCH789", employeeId = userId, accountId = 8938,
                workingDate = today, commuteLogSfid = null, accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(eventScheduleId) } returns Optional.of(tms)
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(tms)

            // When
            val result = attendanceService.register(userId, null, null, eventScheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.attendanceType).isEqualTo(AttendanceType.EVENT)
            assertThat(result.eventScheduleId).isEqualTo(eventScheduleId)
            assertThat(result.scheduleWorkingDate).isEqualTo(today)
            assertThat(result.displayWorkScheduleId).isNull()
            assertThat(result.scheduleStartDate).isNull()
            // SF 레거시 동등: 행사 일정 출근(기존 TMS update = beforeUpdate 트리거)도 환산 일정 재집계
            verify { adminMonthlyIntegrationService.refreshIntegration(userId, YearMonth.from(today)) }
        }

        @Test
        @DisplayName("Spec #587 P2-B T2 — 일정 미존재: eventScheduleId 로 TMS 조회 실패 -> EventScheduleNotFoundException")
        fun register_byEventSchedule_notFound_throwsException() {
            // Given
            val userId = 1L
            val eventScheduleId = 999999L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(eventScheduleId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, null, eventScheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EventScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("Spec #587 P2-B T3 — 타인 일정: TMS.employee.id != currentEmployeeId -> EventScheduleNotAssignedException")
        fun register_byEventSchedule_notAssigned_throwsException() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val eventScheduleId = 789L
            val today = LocalDate.now()

            val employee = createEmployee(id = userId, sfid = "USR001")
            val tms = createTeamMemberSchedule(
                id = eventScheduleId, employeeId = otherUserId, accountId = 8938, workingDate = today
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(eventScheduleId) } returns Optional.of(tms)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, null, eventScheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EventScheduleNotAssignedException::class.java)
        }

        @Test
        @DisplayName("Spec #587 P2-B T4 — 일자 불일치: TMS.working_date != today -> EventScheduleDateMismatchException")
        fun register_byEventSchedule_dateMismatch_throwsException() {
            // Given
            val userId = 1L
            val eventScheduleId = 789L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")
            val tms = createTeamMemberSchedule(
                id = eventScheduleId, employeeId = userId, accountId = 8938,
                workingDate = today.plus(1, ChronoUnit.DAYS), // 미래 일자
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(eventScheduleId) } returns Optional.of(tms)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, null, eventScheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EventScheduleDateMismatchException::class.java)
        }

        @Test
        @DisplayName("Spec #587 P2-B T5 — 이미 출근 등록: TMS.commuteLogSfid NOT NULL -> EventAttendanceDuplicateException")
        fun register_byEventSchedule_alreadyRegistered_throwsException() {
            // Given
            val userId = 1L
            val eventScheduleId = 789L
            val today = LocalDate.now()
            val employee = createEmployee(id = userId, sfid = "USR001")
            val tms = createTeamMemberSchedule(
                id = eventScheduleId, employeeId = userId, accountId = 8938,
                workingDate = today, commuteLogSfid = "OK"
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { teamMemberScheduleRepository.findById(eventScheduleId) } returns Optional.of(tms)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, null, null, eventScheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EventAttendanceDuplicateException::class.java)
        }

        @Test
        @DisplayName("Spec #587 P2-B T6 — 동시 입력: displayWorkScheduleId + eventScheduleId 둘 다 -> AttendanceDualBranchException")
        fun register_byEventSchedule_dualBranch_throwsException() {
            // When & Then — 분기 가드는 employeeRepository 조회 전에 throw
            assertThatThrownBy {
                attendanceService.register(1L, null, 100L, 789L, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AttendanceDualBranchException::class.java)
        }
    }

    // ========== register - 안전점검 데이터 연동 Tests ==========

    @Nested
    @DisplayName("register - 안전점검 데이터 연동")
    inner class RegisterSafetyCheckDataSyncTests {

        private val accountLat = 37.4979
        private val accountLon = 127.0276
        private val nearUserLat = 37.4995
        private val nearUserLon = 127.0300

        @Test
        @DisplayName("출근등록 성공 시 completeWorkYn 'Y' 업데이트 + TMS 안전점검 데이터 반영")
        fun register_withSafetyCheck_updatesCompleteWorkYnAndTms() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val safetyCheck = createSafetyCheckSubmission(
                employeeId = userId,
                workingDate = today,
                equipment1 = "예",
                equipment2 = "해당없음",
                yesCheckCount = 7,
                noCheckCount = 2,
                startTime = LocalDateTime.of(2026, 4, 1, 8, 0),
                completeTime = LocalDateTime.of(2026, 4, 1, 8, 10),
                precaution = "항목1;항목2",
                precautionCheckCount = 2,
                traversalFlag = "Y"
            )

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.of(safetyCheck)
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>()) } answers { firstArg<SafetyCheckSubmission>() }
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then - completeWorkYn 업데이트 검증
            assertThat(safetyCheck.completeWorkYn).isEqualTo("Y")
            verify { safetyCheckSubmissionRepository.save(safetyCheck) }

            // Then - TMS 안전점검 stamp 검증 (managed entity 직접 반영)
            assertThat(teamMemberSchedule.equipment1).isEqualTo("예")
            assertThat(teamMemberSchedule.equipment2).isEqualTo("해당없음")
            assertThat(teamMemberSchedule.equipment3).isNull()
            assertThat(teamMemberSchedule.yesChkCnt).isEqualTo(7.0)
            assertThat(teamMemberSchedule.noChkCnt).isEqualTo(2.0)
            assertThat(teamMemberSchedule.startTime).isEqualTo(LocalDateTime.of(2026, 4, 1, 8, 0))
            assertThat(teamMemberSchedule.completeTime).isEqualTo(LocalDateTime.of(2026, 4, 1, 8, 10))
            assertThat(teamMemberSchedule.precaution).isEqualTo("항목1;항목2")
            assertThat(teamMemberSchedule.precautionChk).isEqualTo(2.0)
            assertThat(teamMemberSchedule.traversalFlag).isEqualTo("Y")
        }

        @Test
        @DisplayName("출근 등록 시 attendance_log 백링크가 managed entity 에 직접 세팅된다 (bulk UPDATE 유실 회귀 방지)")
        fun register_setsAttendanceLogBacklinkOnManagedEntity() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )
            val savedLog = AttendanceLog(id = 777L)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns savedLog
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then — 백링크가 저장된 로그 entity 로 직접 연결 (bulk UPDATE 미사용 — 이후 dirty flush 에도 보존)
            assertThat(teamMemberSchedule.attendanceLog).isSameAs(savedLog)
            // 출근보고시각 — 레거시 CommuteReportDateTime__c = System.now() 정합 (fixed clock = 오늘 10:00)
            assertThat(teamMemberSchedule.commuteReportDatetime)
                .isEqualTo(LocalDate.now().atTime(10, 0))
        }

        @Test
        @DisplayName("SafetyCheckSubmission 미존재 시 데이터 연동 스킵, 출근등록 자체는 성공")
        fun register_withoutSafetyCheck_skipsDataSync() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.empty()
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then - 출근등록 성공
            assertThat(result.scheduleId).isEqualTo(scheduleId)

            // Then - 데이터 연동 스킵 검증 (안전점검 stamp 미반영)
            verify(exactly = 0) { safetyCheckSubmissionRepository.save(any()) }
            assertThat(teamMemberSchedule.equipment1).isNull()
            assertThat(teamMemberSchedule.yesChkCnt).isNull()
            assertThat(teamMemberSchedule.startTime).isNull()
        }

        @Test
        @DisplayName("중복 호출 (completeWorkYn 이미 'Y') - 에러 없이 멱등 처리")
        fun register_alreadyCompleted_idempotent() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val safetyCheck = createSafetyCheckSubmission(
                employeeId = userId,
                workingDate = today,
                completeWorkYn = "Y",
                equipment1 = "예",
                yesCheckCount = 5,
                noCheckCount = 4
            )

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.of(safetyCheck)
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>()) } answers { firstArg<SafetyCheckSubmission>() }
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When - 에러 없이 성공
            val result = attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(safetyCheck.completeWorkYn).isEqualTo("Y")
            verify { safetyCheckSubmissionRepository.save(safetyCheck) }
            assertThat(teamMemberSchedule.equipment1).isEqualTo("예")
            assertThat(teamMemberSchedule.equipment2).isNull()
            assertThat(teamMemberSchedule.yesChkCnt).isEqualTo(5.0)
            assertThat(teamMemberSchedule.noChkCnt).isEqualTo(4.0)
        }

        @Test
        @DisplayName("Int → Double 변환 - null 값은 null 유지")
        fun register_intToDoubleConversion_nullPreserved() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val safetyCheck = createSafetyCheckSubmission(
                employeeId = userId,
                workingDate = today,
                yesCheckCount = null,
                noCheckCount = null,
                precautionCheckCount = null
            )

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogSfid = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today) } returns true
            every { safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns Optional.of(safetyCheck)
            every { teamMemberScheduleRepository.findById(scheduleId) } returns Optional.of(teamMemberSchedule)
            every { attendanceRegistrar.register(any()) } returns AttendanceLog()
            every { safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>()) } answers { firstArg<SafetyCheckSubmission>() }
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns listOf(teamMemberSchedule)

            // When
            attendanceService.register(userId, scheduleId, null, null, nearUserLat, nearUserLon, null)

            // Then - null → null 변환 검증
            assertThat(teamMemberSchedule.yesChkCnt).isNull()
            assertThat(teamMemberSchedule.noChkCnt).isNull()
            assertThat(teamMemberSchedule.precautionChk).isNull()
        }
    }

    // ========== getStatus Tests ==========

    @Nested
    @DisplayName("getStatus - 출근 현황 조회")
    inner class GetStatusTests {

        @Test
        @DisplayName("3건 중 2건 등록 -> totalCount=3, registeredCount=2")
        fun getStatus_threeSchedulesTwoRegistered_returnsCorrectStatus() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogSfid = "OK", workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "이마트 강남점"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogSfid = "OK", workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "홈플러스 서초점"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogSfid = null, workingCategory1 = WorkingCategory1.DISPLAY,
                    accountName = "롯데마트 송파점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules

            // When
            val result = attendanceService.getStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.currentDate).isEqualTo(today.toString())
            assertThat(result.statusList).hasSize(3)

            // 레거시 home.jsp 현황 팝업 정합: 거래처명 오름차순 정렬
            // (롯데마트 송파점 < 이마트 강남점 < 홈플러스 서초점)

            // 미등록 항목
            assertThat(result.statusList[0].scheduleId).isEqualTo(3L)
            assertThat(result.statusList[0].accountName).isEqualTo("롯데마트 송파점")
            assertThat(result.statusList[0].status).isEqualTo("PENDING")

            // 등록 완료 항목
            assertThat(result.statusList[1].scheduleId).isEqualTo(1L)
            assertThat(result.statusList[1].accountName).isEqualTo("이마트 강남점")
            assertThat(result.statusList[1].workCategory).isEqualTo("진열")
            assertThat(result.statusList[1].status).isEqualTo("REGISTERED")

            assertThat(result.statusList[2].scheduleId).isEqualTo(2L)
            assertThat(result.statusList[2].status).isEqualTo("REGISTERED")
        }

        @Test
        @DisplayName("전체 등록 완료 -> 모든 항목 REGISTERED 상태")
        fun getStatus_allRegistered_returnsAllRegistered() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogSfid = "OK", accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogSfid = "OK", accountName = "홈플러스 서초점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules

            // When
            val result = attendanceService.getStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.statusList).allMatch { it.status == "REGISTERED" }
        }

        @Test
        @DisplayName("미등록 상태 -> 모든 항목 PENDING 상태")
        fun getStatus_noneRegistered_returnsAllPending() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogSfid = null, accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogSfid = null, accountName = "홈플러스 서초점")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns teamMemberSchedules

            // When
            val result = attendanceService.getStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).allMatch { it.status == "PENDING" }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException 발생")
        fun getStatus_userNotFound_throwsException() {
            // Given
            val userId = 999L
            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { attendanceService.getStatus(userId) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("오늘 스케줄 없음 -> 빈 현황 반환")
        fun getStatus_noSchedules_returnsEmptyStatus() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today) } returns emptyList()

            // When
            val result = attendanceService.getStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).isEmpty()
        }
    }

    // ========== Helper Factory Methods ==========

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "USR001",
        employeeCode: String = "USR001",
        name: String = "테스트 사용자",
        orgName: String? = "서울지점",
        role: String? = null,
        costCenterCode: String? = null
    ): Employee {
        return Employee(
            id = id,
            sfid = sfid,
            employeeCode = employeeCode,
            name = name,
            orgName = orgName,
            role = role,
            password = "encodedPassword",
            passwordChangeRequired = false,
            costCenterCode = costCenterCode
        )
    }

    private fun createSafetyCheckSubmission(
        id: Long = 1L,
        employeeId: Long? = 1L,
        workingDate: LocalDate = LocalDate.now(),
        equipment1: String? = null,
        equipment2: String? = null,
        equipment3: String? = null,
        equipment4: String? = null,
        equipment5: String? = null,
        equipment6: String? = null,
        equipment7: String? = null,
        equipment8: String? = null,
        equipment9: String? = null,
        yesCheckCount: Int? = null,
        noCheckCount: Int? = null,
        startTime: LocalDateTime? = null,
        completeTime: LocalDateTime? = null,
        precaution: String? = null,
        precautionCheckCount: Int? = null,
        traversalFlag: String? = null,
        completeWorkYn: String? = "N"
    ): SafetyCheckSubmission {
        return SafetyCheckSubmission(
            id = id,
            employeeId = employeeId,
            workingDate = workingDate,
            equipment1 = equipment1,
            equipment2 = equipment2,
            equipment3 = equipment3,
            equipment4 = equipment4,
            equipment5 = equipment5,
            equipment6 = equipment6,
            equipment7 = equipment7,
            equipment8 = equipment8,
            equipment9 = equipment9,
            yesCheckCount = yesCheckCount,
            noCheckCount = noCheckCount,
            startTime = startTime,
            completeTime = completeTime,
            precaution = precaution,
            precautionCheckCount = precautionCheckCount,
            traversalFlag = traversalFlag,
            completeWorkYn = completeWorkYn
        )
    }

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        sfid: String? = "SCH001",
        employeeId: Long? = 1L,
        workingDate: LocalDate = LocalDate.now(),
        workingType: WorkingType? = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.DISPLAY,
        workingCategory2: WorkingCategory2? = null,
        workingCategory3: WorkingCategory3? = null,
        accountId: Long? = 1,
        accountName: String? = "테스트 거래처",
        accountAddress: String? = "서울시 강남구",
        accountAbcTypeCode: String? = null,
        accountLatitude: String? = null,
        accountLongitude: String? = null,
        commuteLogSfid: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employee = employeeId?.let { Employee(id = it, employeeCode = "EMP$it", name = "테스트$it") },
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
            workingCategory2 = workingCategory2,
            workingCategory3 = workingCategory3,
            account = accountId?.let {
                Account(
                    id = it,
                    name = accountName,
                    address1 = accountAddress,
                    abcTypeCode = accountAbcTypeCode,
                    latitude = accountLatitude,
                    longitude = accountLongitude
                )
            },
            commuteLogSfid = commuteLogSfid,
            // Spec #789 정합 — 출근 등록 가드는 attendance_log id-FK 기준. commuteLogSfid 채워진 fixture 는 attendance_log 도 함께 set.
            attendanceLog = commuteLogSfid?.let { AttendanceLog(id = 1L) }
        )
    }

    private fun createDisplayWorkSchedule(
        id: Long = 100L,
        confirmed: Boolean? = true,
        startDate: LocalDate? = LocalDate.now().minus(30, ChronoUnit.DAYS),
        endDate: LocalDate? = LocalDate.now().plus(30, ChronoUnit.DAYS),
        typeOfWork3: TypeOfWork3? = TypeOfWork3.FIXED,
        typeOfWork5: TypeOfWork5? = TypeOfWork5.REGULAR,
        employeeId: Long? = 1L,
        accountId: Long? = 8938,
        accountName: String? = "테스트 거래처",
        accountLatitude: String? = null,
        accountLongitude: String? = null,
        accountAbcTypeCode: String? = null,
        isDeleted: Boolean? = false
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            id = id,
            confirmed = confirmed,
            startDate = startDate,
            endDate = endDate,
            typeOfWork3 = typeOfWork3,
            typeOfWork5 = typeOfWork5,
            employee = employeeId?.let { Employee(id = it, employeeCode = "EMP$it", name = "테스트$it") },
            account = accountId?.let {
                Account(
                    id = it,
                    name = accountName,
                    address1 = "서울시 강남구",
                    abcTypeCode = accountAbcTypeCode,
                    latitude = accountLatitude,
                    longitude = accountLongitude
                )
            },
            isDeleted = isDeleted
        )
    }
}
