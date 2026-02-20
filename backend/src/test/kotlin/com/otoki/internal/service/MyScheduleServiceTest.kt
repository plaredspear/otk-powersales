package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.repository.AttendanceRepository
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("MyScheduleService 테스트")
class MyScheduleServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Mock
    private lateinit var attendanceRepository: AttendanceRepository

    @InjectMocks
    private lateinit var myScheduleService: MyScheduleService

    // ========== 월간 일정 조회 Tests ==========

    @Nested
    @DisplayName("월간 일정 조회")
    inner class GetMonthlySchedule {

        @Test
        @DisplayName("성공 - 근무일이 있는 경우")
        fun getMonthlySchedule_withWorkDays_success() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val workDates = listOf(
                LocalDate.of(2020, 8, 1),
                LocalDate.of(2020, 8, 4),
                LocalDate.of(2020, 8, 10)
            )

            whenever(storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(
                eq(userId),
                eq(LocalDate.of(2020, 8, 1)),
                eq(LocalDate.of(2020, 8, 31))
            )).thenReturn(workDates)

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.year).isEqualTo(2020)
            assertThat(result.month).isEqualTo(8)
            assertThat(result.workDays).hasSize(31) // 8월은 31일
            assertThat(result.workDays.filter { it.hasWork }).hasSize(3)
            assertThat(result.workDays[0].date).isEqualTo("2020-08-01")
            assertThat(result.workDays[0].hasWork).isTrue()
            assertThat(result.workDays[3].date).isEqualTo("2020-08-04")
            assertThat(result.workDays[3].hasWork).isTrue()
            assertThat(result.workDays[1].hasWork).isFalse() // 2020-08-02는 근무일 아님
        }

        @Test
        @DisplayName("성공 - 근무일이 없는 경우")
        fun getMonthlySchedule_noWorkDays_success() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8

            whenever(storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(
                eq(userId),
                any(),
                any()
            )).thenReturn(emptyList())

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.year).isEqualTo(2020)
            assertThat(result.month).isEqualTo(8)
            assertThat(result.workDays).hasSize(31)
            assertThat(result.workDays.all { !it.hasWork }).isTrue()
        }

        @Test
        @DisplayName("성공 - 2월(윤년 아님) 28일 반환")
        fun getMonthlySchedule_february_nonLeapYear() {
            // Given
            val userId = 1L
            val year = 2021
            val month = 2

            whenever(storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(
                eq(userId),
                any(),
                any()
            )).thenReturn(emptyList())

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.workDays).hasSize(28)
        }
    }

    // ========== 일간 일정 상세 조회 Tests ==========

    @Nested
    @DisplayName("일간 일정 상세 조회")
    inner class GetDailySchedule {

        @Test
        @DisplayName("성공 - 전체 미등록")
        fun getDailySchedule_allUnregistered_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117")
            val mockSchedules = listOf(
                createMockSchedule(1L, userId, 100L, "이마트", "진열", date),
                createMockSchedule(2L, userId, 200L, "롯데마트", "진열", date),
                createMockSchedule(3L, userId, 300L, "미광물류", "진열", date)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, date))
                .thenReturn(mockSchedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, date))
                .thenReturn(emptyList())

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.date).isEqualTo("2020-08-04")
            assertThat(result.dayOfWeek).isEqualTo("화")
            assertThat(result.memberName).isEqualTo("최금주")
            assertThat(result.employeeNumber).isEqualTo("20030117")
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(3)
            assertThat(result.reportProgress.workType).isEqualTo("진열")
            assertThat(result.stores).hasSize(3)
            assertThat(result.stores.all { !it.isRegistered }).isTrue()
        }

        @Test
        @DisplayName("성공 - 부분 등록")
        fun getDailySchedule_partiallyRegistered_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117")
            val mockSchedules = listOf(
                createMockSchedule(1L, userId, 100L, "이마트", "진열", date),
                createMockSchedule(2L, userId, 200L, "롯데마트", "진열", date),
                createMockSchedule(3L, userId, 300L, "미광물류", "진열", date)
            )
            val mockAttendances = listOf(
                createMockAttendance(1L, userId, 100L, date) // 100L만 등록됨
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, date))
                .thenReturn(mockSchedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, date))
                .thenReturn(mockAttendances)

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.reportProgress.completed).isEqualTo(1)
            assertThat(result.reportProgress.total).isEqualTo(3)
            assertThat(result.stores[0].isRegistered).isTrue() // storeId=100L
            assertThat(result.stores[1].isRegistered).isFalse() // storeId=200L
            assertThat(result.stores[2].isRegistered).isFalse() // storeId=300L
        }

        @Test
        @DisplayName("성공 - 전체 등록")
        fun getDailySchedule_allRegistered_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117")
            val mockSchedules = listOf(
                createMockSchedule(1L, userId, 100L, "이마트", "진열", date),
                createMockSchedule(2L, userId, 200L, "롯데마트", "진열", date)
            )
            val mockAttendances = listOf(
                createMockAttendance(1L, userId, 100L, date),
                createMockAttendance(2L, userId, 200L, date)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, date))
                .thenReturn(mockSchedules)
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, date))
                .thenReturn(mockAttendances)

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.reportProgress.completed).isEqualTo(2)
            assertThat(result.reportProgress.total).isEqualTo(2)
            assertThat(result.stores.all { it.isRegistered }).isTrue()
        }

        @Test
        @DisplayName("성공 - 일정 없음")
        fun getDailySchedule_noSchedule_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findByUserIdAndScheduleDate(userId, date))
                .thenReturn(emptyList())
            whenever(attendanceRepository.findByUserIdAndAttendanceDate(userId, date))
                .thenReturn(emptyList())

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(0)
            assertThat(result.reportProgress.workType).isEmpty()
            assertThat(result.stores).isEmpty()
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getDailySchedule_userNotFound() {
            // Given
            val userId = 999L
            val date = LocalDate.of(2020, 8, 4)

            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThrows<UserNotFoundException> {
                myScheduleService.getDailySchedule(userId, date)
            }
        }
    }

    // ========== Helper Methods ==========

    private fun createMockUser(userId: Long, name: String, employeeId: String): User {
        return User(
            id = userId,
            employeeId = employeeId,
            password = "encoded",
            name = name,
            orgName = "서울지점"
        )
    }

    private fun createMockSchedule(
        id: Long,
        userId: Long,
        storeId: Long,
        storeName: String,
        workCategory: String,
        scheduleDate: LocalDate
    ): StoreSchedule {
        return StoreSchedule(
            id = id,
            userId = userId,
            storeId = storeId,
            storeName = storeName,
            storeCode = "ST${storeId}",
            workCategory = workCategory,
            scheduleDate = scheduleDate
        )
    }

    private fun createMockAttendance(
        id: Long,
        userId: Long,
        storeId: Long,
        attendanceDate: LocalDate
    ): Attendance {
        return Attendance(
            id = id,
            userId = userId,
            storeId = storeId,
            workType = AttendanceWorkType.ROOM_TEMP,
            attendanceDate = attendanceDate
        )
    }
}
