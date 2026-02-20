package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.AttendanceRepository
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
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
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AttendanceService 테스트")
class AttendanceServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Mock
    private lateinit var attendanceRepository: AttendanceRepository

    @InjectMocks
    private lateinit var attendanceService: AttendanceService

    // ========== getStoreList Tests ==========

    @Nested
    @DisplayName("getStoreList - 오늘 출근 거래처 목록 조회")
    inner class GetStoreListTests {

        @Test
        @DisplayName("당일 스케줄이 있으면 거래처 목록을 반환한다")
        fun getStoreList_withSchedules() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점"),
                createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점"),
                createStoreSchedule(storeId = 103, storeName = "롯데마트 동래점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(emptyList())

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.stores[0].storeName).isEqualTo("이마트 부산점")
            assertThat(result.stores[0].isRegistered).isFalse()
        }

        @Test
        @DisplayName("당일 스케줄이 없으면 빈 목록을 반환한다")
        fun getStoreList_noSchedules() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(emptyList())
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(emptyList())

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.registeredCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일부 거래처가 등록된 상태에서 목록을 조회한다")
        fun getStoreList_withPartialRegistrations() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점"),
                createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점")
            )
            val attendances = listOf(
                createAttendance(storeId = 101, workType = AttendanceWorkType.ROOM_TEMP)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(attendances)

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.stores[0].isRegistered).isTrue()
            assertThat(result.stores[0].registeredWorkType).isEqualTo("ROOM_TEMP")
            assertThat(result.stores[1].isRegistered).isFalse()
            assertThat(result.stores[1].registeredWorkType).isNull()
        }

        @Test
        @DisplayName("키워드 검색으로 거래처를 필터링한다")
        fun getStoreList_withKeyword() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val keyword = "이마트"
            val filteredSchedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(userId, today, keyword))
                .thenReturn(filteredSchedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(emptyList())

            // When
            val result = attendanceService.getStoreList(userId, keyword)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].storeName).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("고정근무자의 거래처 목록을 조회한다")
        fun getStoreList_fixedWorker() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(emptyList())

            // When
            val result = attendanceService.getStoreList(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 UserNotFoundException 발생")
        fun getStoreList_userNotFound() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.getStoreList(userId, null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    // ========== registerAttendance Tests ==========

    @Nested
    @DisplayName("registerAttendance - 출근등록")
    inner class RegisterAttendanceTests {

        @Test
        @DisplayName("정상 출근등록 성공")
        fun registerAttendance_success() {
            // Given
            val userId = 1L
            val storeId = 101L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedule = createStoreSchedule(storeId = storeId, storeName = "이마트 부산점")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(schedule)
            whenever(attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today))
                .thenReturn(false)
            whenever(attendanceRepository.save(any<Attendance>()))
                .thenAnswer { invocation ->
                    val att = invocation.getArgument<Attendance>(0)
                    Attendance(
                        id = 1001,
                        userId = att.userId,
                        storeId = att.storeId,
                        workType = att.workType,
                        attendanceDate = att.attendanceDate,
                        registeredAt = att.registeredAt
                    )
                }
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(listOf(schedule, createStoreSchedule(storeId = 102, storeName = "홈플러스")))
            whenever(attendanceRepository.countByUserIdAndAttendanceDate(userId, today))
                .thenReturn(1)

            // When
            val result = attendanceService.registerAttendance(userId, storeId, "ROOM_TEMP")

            // Then
            assertThat(result.attendanceId).isEqualTo(1001)
            assertThat(result.storeId).isEqualTo(storeId)
            assertThat(result.storeName).isEqualTo("이마트 부산점")
            assertThat(result.workType).isEqualTo("ROOM_TEMP")
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(1)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 - UserNotFoundException 발생")
        fun registerAttendance_userNotFound() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.registerAttendance(userId, 101, "ROOM_TEMP") }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("유효하지 않은 workType - InvalidWorkTypeException 발생")
        fun registerAttendance_invalidWorkType() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            // When & Then
            assertThatThrownBy { attendanceService.registerAttendance(userId, 101, "INVALID") }
                .isInstanceOf(InvalidWorkTypeException::class.java)
        }

        @Test
        @DisplayName("스케줄에 없는 거래처 - StoreNotFoundException 발생")
        fun registerAttendance_storeNotFound() {
            // Given
            val userId = 1L
            val storeId = 999L
            val user = createUser(id = userId)
            val today = LocalDate.now()

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(null)

            // When & Then
            assertThatThrownBy { attendanceService.registerAttendance(userId, storeId, "ROOM_TEMP") }
                .isInstanceOf(StoreNotFoundException::class.java)
        }

        @Test
        @DisplayName("중복 등록 - AlreadyRegisteredException 발생")
        fun registerAttendance_alreadyRegistered() {
            // Given
            val userId = 1L
            val storeId = 101L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedule = createStoreSchedule(storeId = storeId)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(schedule)
            whenever(attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { attendanceService.registerAttendance(userId, storeId, "ROOM_TEMP") }
                .isInstanceOf(AlreadyRegisteredException::class.java)
        }

        @Test
        @DisplayName("격고 근무자 한도 초과 - RegistrationLimitExceededException 발생")
        fun registerAttendance_irregularLimitExceeded() {
            // Given
            val userId = 1L
            val storeId = 103L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedule = createStoreSchedule(storeId = storeId)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(schedule)
            whenever(attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today))
                .thenReturn(false)
            whenever(attendanceRepository.countByUserIdAndAttendanceDate(userId, today))
                .thenReturn(2) // 이미 2건 등록

            // When & Then
            assertThatThrownBy { attendanceService.registerAttendance(userId, storeId, "ROOM_TEMP") }
                .isInstanceOf(RegistrationLimitExceededException::class.java)
        }

        @Test
        @DisplayName("격고 근무자 한도 내 등록 - 성공")
        fun registerAttendance_irregularWithinLimit() {
            // Given
            val userId = 1L
            val storeId = 102L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedule = createStoreSchedule(storeId = storeId, storeName = "홈플러스 서면점")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(schedule)
            whenever(attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today))
                .thenReturn(false)
            whenever(attendanceRepository.countByUserIdAndAttendanceDate(userId, today))
                .thenReturn(1) // 1건만 등록된 상태
            whenever(attendanceRepository.save(any<Attendance>()))
                .thenAnswer { invocation ->
                    val att = invocation.getArgument<Attendance>(0)
                    Attendance(
                        id = 1002,
                        userId = att.userId,
                        storeId = att.storeId,
                        workType = att.workType,
                        attendanceDate = att.attendanceDate,
                        registeredAt = att.registeredAt
                    )
                }
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(listOf(schedule))
            whenever(attendanceRepository.countByUserIdAndAttendanceDate(userId, today))
                .thenReturn(1) // save 전 1건, save 후에도 1건으로 조회됨 (mock)

            // When
            val result = attendanceService.registerAttendance(userId, storeId, "REFRIGERATED")

            // Then
            assertThat(result.attendanceId).isEqualTo(1002)
            assertThat(result.workType).isEqualTo("REFRIGERATED")
        }

        @Test
        @DisplayName("순회 근무자는 등록 한도 제한 없이 등록 가능")
        fun registerAttendance_patrolNoLimit() {
            // Given
            val userId = 1L
            val storeId = 105L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedule = createStoreSchedule(storeId = storeId, storeName = "테스트 매장")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today))
                .thenReturn(schedule)
            whenever(attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today))
                .thenReturn(false)
            // 순회 근무자: countByUserIdAndAttendanceDate 호출되지 않음
            whenever(attendanceRepository.save(any<Attendance>()))
                .thenAnswer { invocation ->
                    val att = invocation.getArgument<Attendance>(0)
                    Attendance(
                        id = 1003,
                        userId = att.userId,
                        storeId = att.storeId,
                        workType = att.workType,
                        attendanceDate = att.attendanceDate,
                        registeredAt = att.registeredAt
                    )
                }
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(listOf(schedule))
            whenever(attendanceRepository.countByUserIdAndAttendanceDate(userId, today))
                .thenReturn(1)

            // When
            val result = attendanceService.registerAttendance(userId, storeId, "ROOM_TEMP")

            // Then
            assertThat(result.attendanceId).isEqualTo(1003)
        }
    }

    // ========== getAttendanceStatus Tests ==========

    @Nested
    @DisplayName("getAttendanceStatus - 출근등록 현황 조회")
    inner class GetAttendanceStatusTests {

        @Test
        @DisplayName("부분 등록 상태 현황 조회")
        fun getAttendanceStatus_partialRegistration() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점"),
                createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점"),
                createStoreSchedule(storeId = 103, storeName = "롯데마트 동래점")
            )
            val attendances = listOf(
                createAttendance(storeId = 101, workType = AttendanceWorkType.ROOM_TEMP)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(attendances)

            // When
            val result = attendanceService.getAttendanceStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.registeredCount).isEqualTo(1)
            assertThat(result.statusList).hasSize(3)
            assertThat(result.statusList[0].status).isEqualTo("COMPLETED")
            assertThat(result.statusList[0].workType).isEqualTo("ROOM_TEMP")
            assertThat(result.statusList[0].registeredAt).isNotNull()
            assertThat(result.statusList[1].status).isEqualTo("PENDING")
            assertThat(result.statusList[1].workType).isNull()
            assertThat(result.statusList[1].registeredAt).isNull()
            assertThat(result.statusList[2].status).isEqualTo("PENDING")
        }

        @Test
        @DisplayName("전체 등록 완료 상태 현황 조회")
        fun getAttendanceStatus_allRegistered() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점"),
                createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점")
            )
            val attendances = listOf(
                createAttendance(storeId = 101, workType = AttendanceWorkType.ROOM_TEMP),
                createAttendance(storeId = 102, workType = AttendanceWorkType.REFRIGERATED)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(attendances)

            // When
            val result = attendanceService.getAttendanceStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(2)
            assertThat(result.statusList).allMatch { it.status == "COMPLETED" }
        }

        @Test
        @DisplayName("미등록 상태 현황 조회")
        fun getAttendanceStatus_noneRegistered() {
            // Given
            val userId = 1L
            val user = createUser(id = userId)
            val today = LocalDate.now()
            val schedules = listOf(
                createStoreSchedule(storeId = 101, storeName = "이마트 부산점"),
                createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, today))
                .thenReturn(schedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, today))
                .thenReturn(emptyList())

            // When
            val result = attendanceService.getAttendanceStatus(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.registeredCount).isEqualTo(0)
            assertThat(result.statusList).allMatch { it.status == "PENDING" }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 - UserNotFoundException 발생")
        fun getAttendanceStatus_userNotFound() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { attendanceService.getAttendanceStatus(userId) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    // ========== Helpers ==========

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "12345678"
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encodedPassword",
            name = "테스트 사용자",
            orgName = "부산지점",
            passwordChangeRequired = false
        )
    }

    private fun createStoreSchedule(
        storeId: Long = 101L,
        storeName: String = "테스트 거래처",
        userId: Long = 1L
    ): StoreSchedule {
        return StoreSchedule(
            userId = userId,
            storeId = storeId,
            storeName = storeName,
            storeCode = "ST-${String.format("%05d", storeId)}",
            workCategory = "진열",
            address = "부산시 해운대구",
            scheduleDate = LocalDate.now()
        )
    }

    private fun createAttendance(
        storeId: Long = 101L,
        workType: AttendanceWorkType = AttendanceWorkType.ROOM_TEMP,
        userId: Long = 1L
    ): Attendance {
        return Attendance(
            id = storeId * 10,
            userId = userId,
            storeId = storeId,
            workType = workType,
            attendanceDate = LocalDate.now(),
            registeredAt = LocalDateTime.now()
        )
    }
}
