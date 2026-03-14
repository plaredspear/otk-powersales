package com.otoki.internal.schedule.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
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
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var ororaApiService: OroraApiService

    @InjectMocks
    private lateinit var attendanceService: AttendanceService

    // ========== getStoreList Tests ==========

    @Nested
    @DisplayName("getStoreList - 오늘 출근 거래처 목록 조회")
    inner class GetStoreListTests {

        @Test
        @DisplayName("오늘 스케줄 3건 - 전체 조회 -> 3건 반환, GPS 좌표 포함")
        fun getStoreList_threeSchedules_returnsThreeStoresWithGps() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001", workingCategory1 = "진열"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", workingCategory1 = "납품"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = "USR001", accountId = "ACC003", workingCategory1 = "진열")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점", latitude = "37.4979", longitude = "127.0276", address = "서울시 강남구"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점", latitude = "37.5000", longitude = "127.0100", address = "서울시 서초구"),
                createAccount(sfid = "ACC003", name = "롯데마트 송파점", latitude = "37.5100", longitude = "127.0500", address = "서울시 송파구")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002", "ACC003"))).thenReturn(accounts)

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.currentDate).isEqualTo(today.toString())

            // GPS 좌표 포함 확인
            val store1 = result.stores[0]
            assertThat(store1.scheduleSfid).isEqualTo("SCH001")
            assertThat(store1.storeName).isEqualTo("이마트 강남점")
            assertThat(store1.latitude).isEqualTo(37.4979)
            assertThat(store1.longitude).isEqualTo(127.0276)
            assertThat(store1.address).isEqualTo("서울시 강남구")
            assertThat(store1.isRegistered).isFalse()
        }

        @Test
        @DisplayName("키워드='이마트' - 3건 중 이마트 포함 결과만 -> 이마트 매장만 반환")
        fun getStoreList_withKeyword_returnsFilteredResults() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = "USR001", accountId = "ACC003")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점"),
                createAccount(sfid = "ACC003", name = "이마트 송파점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002", "ACC003"))).thenReturn(accounts)

            // When
            val result = attendanceService.getStoreList(userId, "이마트")

            // Then
            assertThat(result.stores).hasSize(2)
            assertThat(result.stores.all { it.storeName.contains("이마트") }).isTrue()
            assertThat(result.totalCount).isEqualTo(2)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> UserNotFoundException 발생")
        fun getStoreList_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.getStoreList(userId, null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("오늘 스케줄 없음 -> 빈 목록 반환")
        fun getStoreList_noSchedules_returnsEmptyList() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(emptyList())

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일부 출근 등록 완료(commuteLogId 존재) -> registeredCount 반영")
        fun getStoreList_withPartialRegistrations_returnsCorrectCount() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001", commuteLogId = "OK"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", commuteLogId = null)
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002"))).thenReturn(accounts)

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.stores[0].isRegistered).isTrue()
            assertThat(result.stores[1].isRegistered).isFalse()
        }
    }

    // ========== registerCommute Tests ==========

    @Nested
    @DisplayName("registerCommute - 출근 등록")
    inner class RegisterCommuteTests {

        // 강남역 기준 좌표
        private val storeLat = 37.4979
        private val storeLon = 127.0276

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
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.registerCommute(userId, teamMemberScheduleSfid, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.teamMemberScheduleSfid).isEqualTo(teamMemberScheduleSfid)
            assertThat(result.storeName).isEqualTo("이마트 강남점")
            assertThat(result.distanceKm).isLessThan(0.5)
            assertThat(result.distanceKm).isEqualTo(0.277)
            assertThat(result.workType).isEqualTo("상온")
        }

        @Test
        @DisplayName("거리 초과(1.2km, 허용 0.5km) - 비면제 코드 2110 -> DistanceExceededException")
        fun registerCommute_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, teamMemberScheduleSfid, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("대리점 면제 코드 1110 - 5km 거리 -> 거리 검증 생략, 성공")
        fun registerCommute_exemptCode1110_success() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "대리점A",
                abcTypeCode = "1110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today))
                .thenReturn(listOf(teamMemberSchedule))

            // 5km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val farLat = 37.5429  // ~5km north
            val farLon = 127.0276

            // When
            val result = attendanceService.registerCommute(userId, teamMemberScheduleSfid, farLat, farLon, null)

            // Then
            assertThat(result.teamMemberScheduleSfid).isEqualTo(teamMemberScheduleSfid)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.storeName).isEqualTo("대리점A")
        }

        @Test
        @DisplayName("대리점 면제 코드 1900 - 10km 거리 -> 거리 검증 생략, 성공")
        fun registerCommute_exemptCode1900_success() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                workingType = "냉장", commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "특수거래처B",
                abcTypeCode = "1900",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today))
                .thenReturn(listOf(teamMemberSchedule))

            // 10km 떨어진 좌표 사용 (면제이므로 성공해야 함)
            val veryFarLat = 37.5879  // ~10km north
            val veryFarLon = 127.0276

            // When
            val result = attendanceService.registerCommute(userId, teamMemberScheduleSfid, veryFarLat, veryFarLon, null)

            // Then
            assertThat(result.teamMemberScheduleSfid).isEqualTo(teamMemberScheduleSfid)
            assertThat(result.distanceKm).isEqualTo(0.0)
            assertThat(result.storeName).isEqualTo("특수거래처B")
        }

        @Test
        @DisplayName("비면제 코드 2110 - 1.2km 거리 -> DistanceExceededException")
        fun registerCommute_nonExemptCode2110_exceedsDistance_throwsException() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "일반매장",
                abcTypeCode = "2110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, teamMemberScheduleSfid, farUserLat, farUserLon, null)
            }.isInstanceOf(DistanceExceededException::class.java)
        }

        @Test
        @DisplayName("중복 출근 등록(commuteLogId='OK') -> AlreadyRegisteredException")
        fun registerCommute_alreadyRegistered_throwsException() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                commuteLogId = "OK"
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, teamMemberScheduleSfid, nearUserLat, nearUserLon, null)
            }.isInstanceOf(AlreadyRegisteredException::class.java)
        }

        @Test
        @DisplayName("스케줄 미존재 -> TeamMemberScheduleNotFoundException")
        fun registerCommute_scheduleNotFound_throwsException() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "NONEXISTENT"
            val user = createUser(id = userId, sfid = "USR001")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(null)

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, teamMemberScheduleSfid, nearUserLat, nearUserLon, null)
            }.isInstanceOf(TeamMemberScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> UserNotFoundException")
        fun registerCommute_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                attendanceService.registerCommute(userId, "SCH001", nearUserLat, nearUserLon, null)
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("workType 파라미터 전달 - workType 우선 반환")
        fun registerCommute_withWorkType_returnsProvidedWorkType() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(teamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today))
                .thenReturn(listOf(teamMemberSchedule))

            // When
            val result = attendanceService.registerCommute(userId, teamMemberScheduleSfid, nearUserLat, nearUserLon, "냉장")

            // Then
            assertThat(result.workType).isEqualTo("냉장")
        }

        @Test
        @DisplayName("출근 등록 성공 시 totalCount, registeredCount 정확 반환")
        fun registerCommute_success_returnsCorrectCounts() {
            // Given
            val userId = 1L
            val teamMemberScheduleSfid = "SCH001"
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val targetTeamMemberSchedule = createTeamMemberSchedule(
                sfid = teamMemberScheduleSfid, employeeId = "USR001", accountId = "ACC001",
                workingType = "상온", commuteLogId = null
            )
            val account = createAccount(
                sfid = "ACC001", name = "이마트 강남점",
                abcTypeCode = "2110",
                latitude = storeLat.toString(), longitude = storeLon.toString()
            )

            // 오늘 전체 스케줄 3건 (1건 이미 등록, 1건 지금 등록, 1건 미등록)
            val allTeamMemberSchedules = listOf(
                targetTeamMemberSchedule,
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", commuteLogId = "OK"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = "USR001", accountId = "ACC003", commuteLogId = null)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)).thenReturn(targetTeamMemberSchedule)
            whenever(accountRepository.findBySfid("ACC001")).thenReturn(account)
            doReturn(OroraWorkReportResult("200", "SUCCESS"))
                .whenever(ororaApiService).sendWorkReport(any(), anyOrNull())
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today))
                .thenReturn(allTeamMemberSchedules)

            // When
            val result = attendanceService.registerCommute(userId, teamMemberScheduleSfid, nearUserLat, nearUserLon, null)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            // SCH002 has commuteLogId="OK", SCH001 matches teamMemberScheduleSfid => 2 registered
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
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001", commuteLogId = "OK", workingCategory1 = "진열"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", commuteLogId = "OK", workingCategory1 = "납품"),
                createTeamMemberSchedule(sfid = "SCH003", employeeId = "USR001", accountId = "ACC003", commuteLogId = null, workingCategory1 = "진열")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점"),
                createAccount(sfid = "ACC003", name = "롯데마트 송파점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002", "ACC003"))).thenReturn(accounts)

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.currentDate).isEqualTo(today.toString())
            assertThat(result.statusList).hasSize(3)

            // 등록 완료 항목
            assertThat(result.statusList[0].scheduleSfid).isEqualTo("SCH001")
            assertThat(result.statusList[0].storeName).isEqualTo("이마트 강남점")
            assertThat(result.statusList[0].workCategory).isEqualTo("진열")
            assertThat(result.statusList[0].status).isEqualTo("REGISTERED")

            assertThat(result.statusList[1].scheduleSfid).isEqualTo("SCH002")
            assertThat(result.statusList[1].status).isEqualTo("REGISTERED")

            // 미등록 항목
            assertThat(result.statusList[2].scheduleSfid).isEqualTo("SCH003")
            assertThat(result.statusList[2].storeName).isEqualTo("롯데마트 송파점")
            assertThat(result.statusList[2].status).isEqualTo("PENDING")
        }

        @Test
        @DisplayName("전체 등록 완료 -> 모든 항목 REGISTERED 상태")
        fun getCommuteStatus_allRegistered_returnsAllRegistered() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001", commuteLogId = "OK"),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", commuteLogId = "OK")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002"))).thenReturn(accounts)

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
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            val teamMemberSchedules = listOf(
                createTeamMemberSchedule(sfid = "SCH001", employeeId = "USR001", accountId = "ACC001", commuteLogId = null),
                createTeamMemberSchedule(sfid = "SCH002", employeeId = "USR001", accountId = "ACC002", commuteLogId = null)
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", name = "이마트 강남점"),
                createAccount(sfid = "ACC002", name = "홈플러스 서초점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(teamMemberSchedules)
            whenever(accountRepository.findBySfidIn(listOf("ACC001", "ACC002"))).thenReturn(accounts)

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).allMatch { it.status == "PENDING" }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 -> UserNotFoundException 발생")
        fun getCommuteStatus_userNotFound_throwsException() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.getCommuteStatus(userId) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("오늘 스케줄 없음 -> 빈 현황 반환")
        fun getCommuteStatus_noSchedules_returnsEmptyStatus() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, sfid = "USR001")
            val today = LocalDate.now()

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate("USR001", today)).thenReturn(emptyList())

            // When
            val result = attendanceService.getCommuteStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).isEmpty()
        }
    }

    // ========== Helper Factory Methods ==========

    private fun createUser(
        id: Long = 1L,
        sfid: String? = "USR001",
        employeeId: String = "12345678",
        name: String = "테스트 사용자",
        orgName: String? = "서울지점",
        appAuthority: String? = null
    ): User {
        return User(
            id = id,
            sfid = sfid,
            employeeId = employeeId,
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
        employeeId: String? = "USR001",
        workingDate: LocalDate = LocalDate.now(),
        workingType: String? = "상온",
        workingCategory1: String? = "진열",
        accountId: String? = "ACC001",
        commuteLogId: String? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            id = id,
            sfid = sfid,
            employeeId = employeeId,
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
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
