package com.otoki.internal.schedule.service

import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.exception.AlreadyRegisteredException
import com.otoki.internal.schedule.exception.DistanceExceededException
import com.otoki.internal.schedule.exception.TeamMemberScheduleNotFoundException
import com.otoki.internal.schedule.integration.OroraApiService
import com.otoki.internal.schedule.integration.OroraWorkReportResult
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AttendanceService 테스트")
class AttendanceServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var ororaApiService: OroraApiService

    @InjectMocks
    private lateinit var attendanceService: AttendanceService

    // ========== getAccountList Tests ==========

    @Nested
    @DisplayName("getAccountList - 오늘 출근 거래처 목록 조회")
    inner class GetAccountListTests {

        @Test
        @DisplayName("오늘 스케줄 3건 - 전체 조회 -> 3건 반환, GPS 좌표 포함")
        fun getAccountList_threeSchedules_returnsThreeAccountsWithGps() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory1 = "진열"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory1 = "납품"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, workingCategory1 = "진열")
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점", latitude = "37.4979", longitude = "127.0276", address = "서울시 강남구"),
                createAccount(id = 8939, name = "홈플러스 서초점", latitude = "37.5000", longitude = "127.0100", address = "서울시 서초구"),
                createAccount(id = 8940, name = "롯데마트 송파점", latitude = "37.5100", longitude = "127.0500", address = "서울시 송파구")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939, 8940))).thenReturn(accounts)

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
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
        @DisplayName("workCategory3 매핑 - 스케줄의 workingCategory3가 '고정' -> 응답에 포함")
        fun getAccountList_workCategory3_mapped() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory3 = "고정"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory3 = null)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "테스트 거래처A"),
                createAccount(id = 8939, name = "테스트 거래처B")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점"),
                createAccount(id = 8939, name = "홈플러스 서초점"),
                createAccount(id = 8940, name = "이마트 송파점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939, 8940))).thenReturn(accounts)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점", address = "서울시 강남구 역삼동"),
                createAccount(id = 8939, name = "홈플러스 서초점", address = "서울시 서초구 반포동"),
                createAccount(id = 8940, name = "롯데마트 송파점", address = "서울시 송파구 잠실동")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939, 8940))).thenReturn(accounts)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점", abcTypeCode = "2001"),
                createAccount(id = 8939, name = "홈플러스 서초점", abcTypeCode = "3001")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = userId, accountId = 8940)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "서울마트", address = "경기도 수원시"),
                createAccount(id = 8939, name = "홈플러스 부산점", address = "서울시 강남구"),
                createAccount(id = 8940, name = "롯데마트 대전점", address = "대전시 유성구")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939, 8940))).thenReturn(accounts)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트", address = "서울시", abcTypeCode = null),
                createAccount(id = 8939, name = "홈플러스", address = "부산시", abcTypeCode = "1234")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

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
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.empty())

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(emptyList())

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.accounts).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일부 출근 등록 완료(commuteLogId 존재) -> registeredCount 반영")
        fun getAccountList_withPartialRegistrations_returnsCorrectCount() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = null)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점"),
                createAccount(id = 8939, name = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.accounts[0].isRegistered).isTrue()
            assertThat(result.accounts[1].isRegistered).isFalse()
        }
    }

    // ========== registerCommute Tests ==========

    @Nested
    @DisplayName("registerCommute - 출근 등록")
    inner class RegisterCommuteTests {

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
        @DisplayName("거리 범위 내(0.3km, 허용 0.5km) - 출근 등록 -> 성공")
        fun registerCommute_withinDistance_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.registerCommute(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.accountName).isEqualTo("이마트 강남점")
            assertThat(result.distanceKm).isLessThan(0.5)
            assertThat(result.distanceKm).isEqualTo(0.277)
            assertThat(result.workType).isEqualTo("상온")
        }

        @Test
        @DisplayName("거리 초과(1.2km, 허용 0.5km) - 비면제 코드 2110 -> DistanceExceededException")
        fun registerCommute_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, scheduleId, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("대리점 면제 코드 1110 - 5km 거리 -> 거리 검증 생략, 성공")
        fun registerCommute_exemptCode1110_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "대리점A",
                abcTypeCode = "1110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // 5km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val farLat = 37.5429  // ~5km north
            val farLon = 127.0276

            // When
            val result = attendanceService.registerCommute(userId, scheduleId, farLat, farLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("대리점A")
        }

        @Test
        @DisplayName("대리점 면제 코드 1900 - 10km 거리 -> 거리 검증 생략, 성공")
        fun registerCommute_exemptCode1900_success() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = "냉장", commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "특수거래처B",
                abcTypeCode = "1900",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // 10km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val veryFarLat = 37.5879  // ~10km north
            val veryFarLon = 127.0276

            // When
            val result = attendanceService.registerCommute(userId, scheduleId, veryFarLat, veryFarLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("특수거래처B")
        }

        @Test
        @DisplayName("비면제 코드 2110 - 1.2km 거리 -> DistanceExceededException")
        fun registerCommute_nonExemptCode2110_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "일반매장",
                abcTypeCode = "2110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, scheduleId, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("중복 출근 등록(commuteLogId='OK') -> AlreadyRegisteredException")
        fun registerCommute_alreadyRegistered_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogId = "OK"
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, scheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AlreadyRegisteredException::class.java)
        }

        @Test
        @DisplayName("스케줄 미존재 -> TeamMemberScheduleNotFoundException")
        fun registerCommute_scheduleNotFound_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 99999L
            val employee = createEmployee(id = userId, sfid = "USR001")

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, scheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(TeamMemberScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException")
        fun registerCommute_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, 10L, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("workType 파라미터 전달 - workType 우선 반환")
        fun registerCommute_withWorkType_returnsProvidedWorkType() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.registerCommute(userId, scheduleId, nearUserLat, nearUserLon, "냉장")

            // Then
            assertThat(result.workType).isEqualTo("냉장")
        }

        @Test
        @DisplayName("출근 등록 성공 시 totalCount, registeredCount 정확 반환")
        fun registerCommute_success_returnsCorrectCounts() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val targetTeamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                id = 8938, name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = accountLat.toString(), longitude = accountLon.toString()
            )

            // 오늘 전체 스케줄 3건 (1건 이미 등록, 1건 지금 등록, 1건 미등록)
            val allTeamMemberSchedules = listOf(
                targetTeamMemberSchedule,
                createTeamMemberSchedule(id = 20L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK"),
                createTeamMemberSchedule(id = 30L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogId = null)
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(targetTeamMemberSchedule))
            whenever(accountRepository.findById(8938)).thenReturn(Optional.of(account))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(allTeamMemberSchedules)

            // When
            val result = attendanceService.registerCommute(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            // id=20 has commuteLogId="OK", id=10 matches scheduleId => 2 registered
            assertThat(result.registeredCount).isEqualTo(2)
        }
    }

    // ========== getCommuteStatus Tests ==========

    @Nested
    @DisplayName("getCommuteStatus - 출근 현황 조회")
    inner class GetCommuteStatusTests {

        @Test
        @DisplayName("3건 중 2건 등록 -> totalCount=3, registeredCount=2")
        fun getCommuteStatus_threeSchedulesTwoRegistered_returnsCorrectStatus() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK", workingCategory1 = "진열"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK", workingCategory1 = "납품"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogId = null, workingCategory1 = "진열")
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점"),
                createAccount(id = 8939, name = "홈플러스 서초점"),
                createAccount(id = 8940, name = "롯데마트 송파점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939, 8940))).thenReturn(accounts)

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.currentDate).isEqualTo(today.toString())
            assertThat(result.statusList).hasSize(3)

            // 등록 완료 항목
            assertThat(result.statusList[0].scheduleId).isEqualTo(1L)
            assertThat(result.statusList[0].accountName).isEqualTo("이마트 강남점")
            assertThat(result.statusList[0].workCategory).isEqualTo("진열")
            assertThat(result.statusList[0].status).isEqualTo("REGISTERED")

            assertThat(result.statusList[1].scheduleId).isEqualTo(2L)
            assertThat(result.statusList[1].status).isEqualTo("REGISTERED")

            // 미등록 항목
            assertThat(result.statusList[2].scheduleId).isEqualTo(3L)
            assertThat(result.statusList[2].accountName).isEqualTo("롯데마트 송파점")
            assertThat(result.statusList[2].status).isEqualTo("PENDING")
        }

        @Test
        @DisplayName("전체 등록 완료 -> 모든 항목 REGISTERED 상태")
        fun getCommuteStatus_allRegistered_returnsAllRegistered() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK")
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점"),
                createAccount(id = 8939, name = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.statusList).allMatch { it.status == "REGISTERED" }
        }

        @Test
        @DisplayName("미등록 상태 -> 모든 항목 PENDING 상태")
        fun getCommuteStatus_noneRegistered_returnsAllPending() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = null),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = null)
            )

            val accounts = listOf(
                createAccount(id = 8938, name = "이마트 강남점"),
                createAccount(id = 8939, name = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findByIdIn(listOf(8938, 8939))).thenReturn(accounts)

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).allMatch { it.status == "PENDING" }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException 발생")
        fun getCommuteStatus_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.getCommuteStatus(userId) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("오늘 스케줄 없음 -> 빈 현황 반환")
        fun getCommuteStatus_noSchedules_returnsEmptyStatus() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(emptyList())

            // When
            val result = attendanceService.getCommuteStatus(userId)

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
        appAuthority: String? = null
    ): Employee {
        return Employee(
            id = id,
            sfid = sfid,
            employeeCode = employeeCode,
            name = name,
            orgName = orgName,
            appAuthority = appAuthority,
            password = "encodedPassword",
            passwordChangeRequired = false
        )
    }

    private fun createTeamMemberSchedule(
        id: Long = 0L,
        sfid: String? = "SCH001",
        employeeId: Long? = 1L,
        workingDate: LocalDate = LocalDate.now(),
        workingType: String? = "상온",
        workingCategory1: String? = "진열",
        workingCategory3: String? = null,
        accountId: Int? = 1,
        commuteLogId: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employeeId = employeeId,
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
            workingCategory3 = workingCategory3,
            accountId = accountId,
            commuteLogId = commuteLogId
        )
    }

    private fun createAccount(
        id: Int = 0,
        sfid: String? = "ACC001",
        name: String? = "테스트 거래처",
        address: String? = "서울시 강남구",
        abcTypeCode: String? = null,
        latitude: String? = null,
        longitude: String? = null
    ): Account {
        return Account(
            id = id,
            sfid = sfid,
            name = name,
            address1 = address,
            abcTypeCode = abcTypeCode,
            latitude = latitude,
            longitude = longitude
        )
    }
}
