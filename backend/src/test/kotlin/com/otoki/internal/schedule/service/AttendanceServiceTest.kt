package com.otoki.internal.schedule.service

import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import com.otoki.internal.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.exception.AlreadyRegisteredException
import com.otoki.internal.schedule.exception.DistanceExceededException
import com.otoki.internal.schedule.exception.SafetyCheckRequiredException
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AttendanceService 테스트")
class AttendanceServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository

    @Mock
    private lateinit var ororaApiService: OroraApiService

    @InjectMocks
    private lateinit var attendanceService: AttendanceService

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
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory1 = "진열",
                    accountName = "이마트 강남점", accountLatitude = "37.4979", accountLongitude = "127.0276", accountAddress = "서울시 강남구"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory1 = "납품",
                    accountName = "홈플러스 서초점", accountLatitude = "37.5000", accountLongitude = "127.0100", accountAddress = "서울시 서초구"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, workingCategory1 = "진열",
                    accountName = "롯데마트 송파점", accountLatitude = "37.5100", accountLongitude = "127.0500", accountAddress = "서울시 송파구")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(false)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, workingCategory3 = "고정",
                    accountName = "테스트 거래처A"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, workingCategory3 = null,
                    accountName = "테스트 거래처B")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(false)
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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK", accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = null, accountName = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

            // When
            val result = attendanceService.getAccountList(userId, null)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.accounts[0].isRegistered).isTrue()
            assertThat(result.accounts[1].isRegistered).isFalse()
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
                workingType = "상온", commuteLogId = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.accountName).isEqualTo("이마트 강남점")
            assertThat(result.distanceKm).isLessThan(0.5)
            assertThat(result.distanceKm).isEqualTo(0.277)
            assertThat(result.workType).isEqualTo("상온")
        }

        @Test
        @DisplayName("안전점검 미완료 - 출근 등록 시도 -> SafetyCheckRequiredException")
        fun register_safetyCheckNotCompleted_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(false)

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)
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
                commuteLogId = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, farUserLat, farUserLon, null)
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
                workingType = "상온", commuteLogId = null,
                accountName = "대리점A", accountAbcTypeCode = "1110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // 5km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val farLat = 37.5429  // ~5km north
            val farLon = 127.0276

            // When
            val result = attendanceService.register(userId, scheduleId, farLat, farLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("대리점A")
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
                workingType = "냉장", commuteLogId = null,
                accountName = "특수거래처B", accountAbcTypeCode = "1900",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // 10km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val veryFarLat = 37.5879  // ~10km north
            val veryFarLon = 127.0276

            // When
            val result = attendanceService.register(userId, scheduleId, veryFarLat, veryFarLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.accountName).isEqualTo("특수거래처B")
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
                commuteLogId = null,
                accountName = "일반매장", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("중복 출근 등록(commuteLogId='OK') -> AlreadyRegisteredException")
        fun register_alreadyRegistered_throwsException() {
            // Given
            val userId = 1L
            val scheduleId = 10L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                id = scheduleId, sfid = "SCH001", employeeId = userId, accountId = 8938,
                commuteLogId = "OK"
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)
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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)
            }.isInstanceOf(TeamMemberScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> EmployeeNotFoundException")
        fun register_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                attendanceService.register(userId, 10L, nearUserLat, nearUserLon, null)
            }.isInstanceOf(EmployeeNotFoundException::class.java)
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
                workingType = "상온", commuteLogId = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, "냉장")

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
                workingType = "상온", commuteLogId = null,
                accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            // 오늘 전체 스케줄 3건 (1건 이미 등록, 1건 지금 등록, 1건 미등록)
            val allTeamMemberSchedules = listOf(
                targetTeamMemberSchedule,
                createTeamMemberSchedule(id = 20L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK"),
                createTeamMemberSchedule(id = 30L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogId = null)
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(targetTeamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(allTeamMemberSchedules)

            // When
            val result = attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            // id=20 has commuteLogId="OK", id=10 matches scheduleId => 2 registered
            assertThat(result.registeredCount).isEqualTo(2)
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
                commuteLogId = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.of(safetyCheck))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>())).thenAnswer { it.getArgument<SafetyCheckSubmission>(0) }
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then - completeWorkYn 업데이트 검증
            assertThat(safetyCheck.completeWorkYn).isEqualTo("Y")
            verify(safetyCheckSubmissionRepository).save(safetyCheck)

            // Then - TMS 안전점검 데이터 반영 검증
            verify(teamMemberScheduleRepository).updateSafetyCheckData(
                id = 10L,
                equipment1 = "예",
                equipment2 = "해당없음",
                equipment3 = null,
                equipment4 = null,
                equipment5 = null,
                equipment6 = null,
                equipment7 = null,
                equipment8 = null,
                equipment9 = null,
                yesChkCnt = 7.0,
                noChkCnt = 2.0,
                startTime = LocalDateTime.of(2026, 4, 1, 8, 0),
                completeTime = LocalDateTime.of(2026, 4, 1, 8, 10),
                precaution = "항목1;항목2",
                precautionChk = 2.0,
                traversalFlag = "Y"
            )
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
                commuteLogId = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.empty())
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then - 출근등록 성공
            assertThat(result.scheduleId).isEqualTo(scheduleId)

            // Then - 데이터 연동 스킵 검증
            verify(safetyCheckSubmissionRepository, never()).save(any())
            verify(teamMemberScheduleRepository, never()).updateSafetyCheckData(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )
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
                commuteLogId = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.of(safetyCheck))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>())).thenAnswer { it.getArgument<SafetyCheckSubmission>(0) }
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When - 에러 없이 성공
            val result = attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.scheduleId).isEqualTo(scheduleId)
            assertThat(safetyCheck.completeWorkYn).isEqualTo("Y")
            verify(safetyCheckSubmissionRepository).save(safetyCheck)
            verify(teamMemberScheduleRepository).updateSafetyCheckData(
                id = 10L,
                equipment1 = "예",
                equipment2 = null,
                equipment3 = null,
                equipment4 = null,
                equipment5 = null,
                equipment6 = null,
                equipment7 = null,
                equipment8 = null,
                equipment9 = null,
                yesChkCnt = 5.0,
                noChkCnt = 4.0,
                startTime = null,
                completeTime = null,
                precaution = null,
                precautionChk = null,
                traversalFlag = null
            )
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
                commuteLogId = null, accountName = "이마트 강남점", accountAbcTypeCode = "2110",
                accountLatitude = accountLat.toString(), accountLongitude = accountLon.toString()
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(userId, today)).thenReturn(true)
            whenever(safetyCheckSubmissionRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(Optional.of(safetyCheck))
            whenever(teamMemberScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(teamMemberSchedule))
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any())
            whenever(safetyCheckSubmissionRepository.save(any<SafetyCheckSubmission>())).thenAnswer { it.getArgument<SafetyCheckSubmission>(0) }
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            attendanceService.register(userId, scheduleId, nearUserLat, nearUserLon, null)

            // Then - null → null 변환 검증
            verify(teamMemberScheduleRepository).updateSafetyCheckData(
                id = 10L,
                equipment1 = null,
                equipment2 = null,
                equipment3 = null,
                equipment4 = null,
                equipment5 = null,
                equipment6 = null,
                equipment7 = null,
                equipment8 = null,
                equipment9 = null,
                yesChkCnt = null,
                noChkCnt = null,
                startTime = null,
                completeTime = null,
                precaution = null,
                precautionChk = null,
                traversalFlag = null
            )
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
                createTeamMemberSchedule(id = 1L, sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK", workingCategory1 = "진열",
                    accountName = "이마트 강남점"),
                createTeamMemberSchedule(id = 2L, sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK", workingCategory1 = "납품",
                    accountName = "홈플러스 서초점"),
                createTeamMemberSchedule(id = 3L, sfid = "SCH003", employeeId = userId, accountId = 8940, commuteLogId = null, workingCategory1 = "진열",
                    accountName = "롯데마트 송파점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

            // When
            val result = attendanceService.getStatus(userId)

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
        fun getStatus_allRegistered_returnsAllRegistered() {
            // Given
            val userId = 1L
            val employee = createEmployee(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = "OK", accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = "OK", accountName = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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
                createTeamMemberSchedule(sfid = "SCH001", employeeId = userId, accountId = 8938, commuteLogId = null, accountName = "이마트 강남점"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = userId, accountId = 8939, commuteLogId = null, accountName = "홈플러스 서초점")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(teamMemberSchedules)

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
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.empty())

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

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, today)).thenReturn(emptyList())

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
        workingType: String? = "상온",
        workingCategory1: String? = "진열",
        workingCategory3: String? = null,
        accountId: Int? = 1,
        accountName: String? = "테스트 거래처",
        accountAddress: String? = "서울시 강남구",
        accountAbcTypeCode: String? = null,
        accountLatitude: String? = null,
        accountLongitude: String? = null,
        commuteLogId: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employee = employeeId?.let { Employee(id = it, employeeCode = "EMP$it", name = "테스트$it") },
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
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
            commuteLogId = commuteLogId
        )
    }
}
