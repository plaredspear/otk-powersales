package com.otoki.internal.common.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.safetycheck.dto.response.SafetyCheckTodayResponse
import com.otoki.internal.safetycheck.service.SafetyCheckService
import com.otoki.internal.teammemberschedule.entity.TeamMemberSchedule
import com.otoki.internal.teammemberschedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.shelflife.repository.ShelfLifeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("HomeService 테스트")
class HomeServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var safetyCheckService: SafetyCheckService

    @Mock
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @InjectMocks
    private lateinit var homeService: HomeService

    @Nested
    @DisplayName("getHomeData - 홈 화면 데이터 조회")
    inner class GetHomeDataTests {

        // ========== 역할별 스케줄 조회 ==========

        @Test
        @DisplayName("여사원 - 본인 스케줄만 조회 -> 본인 employeeId로 조회된 스케줄만 반환")
        fun user_onlyOwnSchedules() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)
            val account = createAccount(sfid = "ACC001", name = "이마트 부산점")

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userSfid, accountId = "ACC001", workingCategory1 = "진열"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userSfid, accountId = "ACC001", workingCategory1 = "행사")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(eq(userSfid), any()))
                .thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(any())).thenReturn(listOf(account))
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = false))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(2)
            assertThat(result.todaySchedules).allMatch { it.employeeSfid == userSfid }
            assertThat(result.todaySchedules[0].storeName).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("조장 - 팀 전체 스케줄 조회 -> orgName 기반 팀원 전체 스케줄 반환")
        fun leader_teamSchedules() {
            // Given
            val userId = 1L
            val leaderSfid = "a0B000000099999"
            val member1Sfid = "a0B000000011111"
            val member2Sfid = "a0B000000022222"
            val orgName = "부산1지점"

            val leader = createUser(id = userId, sfid = leaderSfid, orgName = orgName, appAuthority = "조장")
            val member1 = createUser(id = 2L, sfid = member1Sfid, orgName = orgName, name = "김영희")
            val member2 = createUser(id = 3L, sfid = member2Sfid, orgName = orgName, name = "박미나")
            val teamUsers = listOf(leader, member1, member2)

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = member1Sfid, accountId = "ACC001", workingCategory1 = "진열"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = member2Sfid, accountId = "ACC002", workingCategory1 = "행사"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = leaderSfid, accountId = "ACC001", workingCategory1 = "진열")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 부산점"),
                createAccount(sfid = "ACC002", name = "홈플러스 해운대점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(leader))
            whenever(userRepository.findByOrgName(orgName)).thenReturn(teamUsers)
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(any(), any()))
                .thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(any())).thenReturn(accounts)
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(3)
            val employeeSfids = result.todaySchedules.map { it.employeeSfid }
            assertThat(employeeSfids).containsExactlyInAnyOrder(member1Sfid, member2Sfid, leaderSfid)
        }

        // ========== 안전점검 ==========

        @Test
        @DisplayName("여사원 안전점검 미완료 - 오늘 안전점검 미완료 -> safetyCheckRequired=true")
        fun user_safetyCheckNotCompleted() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "a0B000000012345", appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(userId))
                .thenReturn(SafetyCheckTodayResponse(completed = false))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

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
            val user = createUser(id = userId, sfid = "a0B000000012345", appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(userId))
                .thenReturn(SafetyCheckTodayResponse(completed = true, submittedAt = LocalDateTime.now()))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

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
            val leader = createUser(id = userId, sfid = "a0B000000099999", orgName = "부산1지점", appAuthority = "조장")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(leader))
            whenever(userRepository.findByOrgName("부산1지점")).thenReturn(listOf(leader))
            whenever(teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(any(), any()))
                .thenReturn(emptyList())
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.safetyCheckRequired).isFalse()
        }

        // ========== 유통기한 임박 알림 ==========

        @Test
        @DisplayName("유통기한 알림 - sfid로 오늘 알람 3건 -> expiryCount=3, 프로필 정보 포함")
        fun expiryAlert_withCount() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())
            whenever(shelfLifeRepository.countByEmployeeIdAndAlarmDate(eq(userSfid), any()))
                .thenReturn(3L)

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(3)
            assertThat(result.expiryAlert!!.branchName).isEqualTo("부산1지점")
            assertThat(result.expiryAlert!!.employeeName).isEqualTo("최금주")
            assertThat(result.expiryAlert!!.employeeId).isEqualTo("20030117")
        }

        @Test
        @DisplayName("유통기한 알림 0건 - 오늘 알람 0건 -> expiryAlert not null, expiryCount=0")
        fun expiryAlert_zeroCount() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())
            whenever(shelfLifeRepository.countByEmployeeIdAndAlarmDate(eq(userSfid), any()))
                .thenReturn(0L)

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(0)
        }

        @Test
        @DisplayName("유통기한 알림 sfid null - sfid 없는 사용자 -> expiryCount=0")
        fun expiryAlert_sfidNull() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = null, appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.expiryCount).isEqualTo(0)
        }

        @Test
        @DisplayName("유통기한 알림 orgName null - orgName 없는 사용자 -> branchName 빈 문자열")
        fun expiryAlert_orgNameNull() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, orgName = null, appAuthority = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()))
                .thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())
            whenever(shelfLifeRepository.countByEmployeeIdAndAlarmDate(eq(userSfid), any()))
                .thenReturn(1L)

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.expiryAlert).isNotNull
            assertThat(result.expiryAlert!!.branchName).isEqualTo("")
        }

        // ========== 정렬 ==========

        @Test
        @DisplayName("4단계 정렬 - 출근완료(0) -> 임시배정(1) -> 행사(2) -> 진열(3) 순서로 정렬")
        fun schedules_sortedByPriority() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)

            // 진열 (priority 3)
            val displaySchedule = createTeamMemberSchedule(
                sfid = "SCH_DISPLAY",
                employeeId = userSfid,
                accountId = "ACC001",
                workingCategory1 = "진열",
                workingCategory2 = null,
                commuteLogId = null
            )
            // 행사 (priority 2)
            val eventSchedule = createTeamMemberSchedule(
                sfid = "SCH_EVENT",
                employeeId = userSfid,
                accountId = "ACC002",
                workingCategory1 = "행사",
                workingCategory2 = null,
                commuteLogId = null
            )
            // 임시배정 (priority 1)
            val tempSchedule = createTeamMemberSchedule(
                sfid = "SCH_TEMP",
                employeeId = userSfid,
                accountId = "ACC003",
                workingCategory1 = "진열",
                workingCategory2 = "임시배정",
                commuteLogId = null
            )
            // 출근완료 (priority 0)
            val commuteSchedule = createTeamMemberSchedule(
                sfid = "SCH_COMMUTE",
                employeeId = userSfid,
                accountId = "ACC004",
                workingCategory1 = "진열",
                workingCategory2 = null,
                commuteLogId = "CLG001"
            )

            // 의도적으로 역순 전달 (진열 -> 행사 -> 임시 -> 출근완료)
            val teamMemberSchedules = listOf(displaySchedule, eventSchedule, tempSchedule, commuteSchedule)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(eq(userSfid), any()))
                .thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(any())).thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(4)
            assertThat(result.todaySchedules[0].scheduleId).isEqualTo("SCH_COMMUTE")
            assertThat(result.todaySchedules[1].scheduleId).isEqualTo("SCH_TEMP")
            assertThat(result.todaySchedules[2].scheduleId).isEqualTo("SCH_EVENT")
            assertThat(result.todaySchedules[3].scheduleId).isEqualTo("SCH_DISPLAY")
        }

        // ========== 중복 제거 ==========

        @Test
        @DisplayName("중복 사원 제거 - 동일 sfid 스케줄이 복수 존재 -> 정렬 후 첫 번째만 반환")
        fun schedules_deduplicatedBySfid() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)

            // 같은 sfid로 진열(priority 3)과 행사(priority 2) 스케줄 존재
            val displaySchedule = createTeamMemberSchedule(
                sfid = "SCH_SAME",
                employeeId = userSfid,
                accountId = "ACC001",
                workingCategory1 = "진열",
                commuteLogId = null
            )
            val eventSchedule = createTeamMemberSchedule(
                sfid = "SCH_SAME",
                employeeId = userSfid,
                accountId = "ACC002",
                workingCategory1 = "행사",
                commuteLogId = null
            )
            val otherSchedule = createTeamMemberSchedule(
                sfid = "SCH_OTHER",
                employeeId = userSfid,
                accountId = "ACC003",
                workingCategory1 = "진열",
                commuteLogId = null
            )

            val teamMemberSchedules = listOf(displaySchedule, eventSchedule, otherSchedule)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(eq(userSfid), any()))
                .thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(any())).thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.todaySchedules).hasSize(2)
            val scheduleIds = result.todaySchedules.map { it.scheduleId }
            assertThat(scheduleIds).containsExactlyInAnyOrder("SCH_SAME", "SCH_OTHER")
        }

        // ========== 출근현황 집계 ==========

        @Test
        @DisplayName("출근현황 집계 - 전체 건수와 출근 등록 건수 -> attendanceSummary 정확히 반환")
        fun attendanceSummary_correctCounts() {
            // Given
            val userId = 1L
            val userSfid = "a0B000000012345"
            val user = createUser(id = userId, sfid = userSfid, appAuthority = null)

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userSfid, accountId = "ACC001", commuteLogId = "CLG001"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userSfid, accountId = "ACC002", commuteLogId = "CLG002"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userSfid, accountId = "ACC003", commuteLogId = null)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(eq(userSfid), any()))
                .thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(any())).thenReturn(emptyList())
            whenever(safetyCheckService.getTodayStatus(any()))
                .thenReturn(SafetyCheckTodayResponse(completed = true))
            whenever(noticeRepository.findRecentNotices(any(), any()))
                .thenReturn(emptyList())

            // When
            val result = homeService.getHomeData(userId)

            // Then
            assertThat(result.attendanceSummary.totalCount).isEqualTo(3)
            assertThat(result.attendanceSummary.registeredCount).isEqualTo(2)
        }

        // ========== 에러 케이스 ==========

        @Test
        @DisplayName("사용자 없음 - 존재하지 않는 userId -> UserNotFoundException 발생")
        fun userNotFound_throwsException() {
            // Given
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { homeService.getHomeData(999L) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    // ========== Helper Factories ==========

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030117",
        name: String = "최금주",
        orgName: String? = "부산1지점",
        sfid: String? = null,
        appAuthority: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encoded_password",
            name = name,
            orgName = orgName,
            sfid = sfid,
            appAuthority = appAuthority
        )
    }

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        sfid: String? = null,
        employeeId: String? = null,
        accountId: String? = null,
        workingDate: LocalDate = LocalDate.now(),
        workingType: String? = "순회",
        workingCategory1: String? = "진열",
        workingCategory2: String? = null,
        commuteLogId: String? = null,
        commuteReportDatetime: LocalDateTime? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employeeId = employeeId,
            accountId = accountId,
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
            workingCategory2 = workingCategory2,
            commuteLogId = commuteLogId,
            commuteReportDatetime = commuteReportDatetime
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
