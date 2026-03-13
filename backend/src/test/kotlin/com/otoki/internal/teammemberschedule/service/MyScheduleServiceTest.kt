package com.otoki.internal.teammemberschedule.service

import com.otoki.internal.teammemberschedule.entity.*
import com.otoki.internal.common.entity.*
import com.otoki.internal.sap.entity.*
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.teammemberschedule.repository.DisplayWorkScheduleRepository
import com.otoki.internal.sap.repository.UserRepository
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
    private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepository

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
            val mockUser = createMockUser(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val workDates = listOf(
                LocalDate.of(2020, 8, 1),
                LocalDate.of(2020, 8, 4),
                LocalDate.of(2020, 8, 10)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(displayWorkScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(
                eq("a0B000000012345"),
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
            assertThat(result.workDays[1].hasWork).isFalse()
        }

        @Test
        @DisplayName("성공 - 근무일이 없는 경우")
        fun getMonthlySchedule_noWorkDays_success() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val mockUser = createMockUser(userId, "최금주", "20030117", sfid = "a0B000000012345")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(displayWorkScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(
                eq("a0B000000012345"),
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
            val mockUser = createMockUser(userId, "최금주", "20030117", sfid = "a0B000000012345")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(displayWorkScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(
                eq("a0B000000012345"),
                any(),
                any()
            )).thenReturn(emptyList())

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.workDays).hasSize(28)
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getMonthlySchedule_userNotFound() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThrows<UserNotFoundException> {
                myScheduleService.getMonthlySchedule(userId, 2020, 8)
            }
        }
    }

    // ========== 일간 일정 상세 조회 Tests ==========

    @Nested
    @DisplayName("일간 일정 상세 조회")
    inner class GetDailySchedule {

        @Test
        @DisplayName("성공 - 일정 있음")
        fun getDailySchedule_withSchedules_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val mockSchedules = listOf(
                createMockSchedule(account = "ACC001", typeOfWork1 = "진열", startDate = date),
                createMockSchedule(account = "ACC002", typeOfWork1 = "진열", startDate = date),
                createMockSchedule(account = "ACC003", typeOfWork1 = "진열", startDate = date)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(displayWorkScheduleRepository.findByFullNameAndStartDate("a0B000000012345", date))
                .thenReturn(mockSchedules)

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
        @DisplayName("성공 - 일정 없음")
        fun getDailySchedule_noSchedule_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockUser(userId, "최금주", "20030117", sfid = "a0B000000012345")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(displayWorkScheduleRepository.findByFullNameAndStartDate("a0B000000012345", date))
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

    private fun createMockUser(userId: Long, name: String, employeeId: String, sfid: String? = null): User {
        return User(
            id = userId,
            employeeId = employeeId,
            password = "encoded",
            name = name,
            orgName = "서울지점",
            sfid = sfid
        )
    }

    private fun createMockSchedule(
        id: Long = 0,
        account: String = "ACC001",
        typeOfWork1: String = "진열",
        startDate: LocalDate = LocalDate.now()
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            id = id,
            fullName = "a0B000000012345",
            account = account,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
