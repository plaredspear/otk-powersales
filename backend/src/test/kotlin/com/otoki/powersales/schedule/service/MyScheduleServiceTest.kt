package com.otoki.powersales.schedule.service

import com.otoki.powersales.schedule.entity.*
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.enums.TypeOfWork1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@DisplayName("MyScheduleService 테스트")
class MyScheduleServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val myScheduleService = MyScheduleService(
        employeeRepository,
        displayWorkScheduleRepository,
        teamMemberScheduleRepository,
    )

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
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val workDates = listOf(
                LocalDate.of(2020, 8, 1),
                LocalDate.of(2020, 8, 4),
                LocalDate.of(2020, 8, 10)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId),
                eq(LocalDate.of(2020, 8, 1)),
                eq(LocalDate.of(2020, 8, 31))
            ) } returns workDates
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

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
            assertThat(result.annualLeaveCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 근무일이 없는 경우")
        fun getMonthlySchedule_noWorkDays_success() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId),
                any(),
                any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.year).isEqualTo(2020)
            assertThat(result.month).isEqualTo(8)
            assertThat(result.workDays).hasSize(31)
            assertThat(result.workDays.all { !it.hasWork }).isTrue()
            assertThat(result.annualLeaveCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 2월(윤년 아님) 28일 반환")
        fun getMonthlySchedule_february_nonLeapYear() {
            // Given
            val userId = 1L
            val year = 2021
            val month = 2
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId),
                any(),
                any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.workDays).hasSize(28)
        }

        @Test
        @DisplayName("성공 - 대휴 2건, 연차 1건 → substituteHolidayCount=2, annualLeaveCount=1")
        fun getMonthlySchedule_withSubstituteHoliday() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 4
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId), any(), any()
            ) } returns listOf(LocalDate.of(2026, 4, 1))
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                any()
            ) } returns listOf(
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 5), workingType = WorkingType.ALT_HOLIDAY),
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 12), workingType = WorkingType.ALT_HOLIDAY),
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 20), workingType = WorkingType.ANNUAL_LEAVE)
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.substituteHolidayCount).isEqualTo(2)
            assertThat(result.annualLeaveCount).isEqualTo(1)
        }

        @Test
        @DisplayName("성공 - 대휴 0건 → substituteHolidayCount=0")
        fun getMonthlySchedule_noSubstituteHoliday() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 4
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId), any(), any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.substituteHolidayCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 연차 2건 있는 월 → annualLeaveCount=2, workingType 매핑")
        fun getMonthlySchedule_withAnnualLeave() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 3
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(
                eq(userId), any(), any()
            ) } returns listOf(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 10))
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                any()
            ) } returns listOf(
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 5), WorkingType.ANNUAL_LEAVE),
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 10), WorkingType.WORK),
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 20), WorkingType.ANNUAL_LEAVE)
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.annualLeaveCount).isEqualTo(2)
            // 3/5 → workingType = WorkingType.ANNUAL_LEAVE
            val day5 = result.workDays.first { it.date == "2026-03-05" }
            assertThat(day5.workingType).isEqualTo("연차")
            assertThat(day5.hasWork).isTrue()
            // 3/10 → workingType = WorkingType.WORK
            val day10 = result.workDays.first { it.date == "2026-03-10" }
            assertThat(day10.workingType).isEqualTo("근무")
            // 스케줄 없는 날 → workingType=null
            val day1 = result.workDays.first { it.date == "2026-03-01" }
            assertThat(day1.workingType).isNull()
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getMonthlySchedule_userNotFound() {
            // Given
            val userId = 999L
            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThrows<EmployeeNotFoundException> {
                myScheduleService.getMonthlySchedule(userId, 2020, 8)
            }
        }
    }

    // ========== 일간 일정 상세 조회 Tests ==========

    @Nested
    @DisplayName("일간 일정 상세 조회")
    inner class GetDailySchedule {

        @Test
        @DisplayName("성공 - 일정 있음 (일반 근무)")
        fun getDailySchedule_withSchedules_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val mockSchedules = listOf(
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, startDate = date),
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, startDate = date),
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, startDate = date)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.WORK))
            every { displayWorkScheduleRepository.findByEmployeeAndStartDate(userId, date) } returns mockSchedules

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.date).isEqualTo("2020-08-04")
            assertThat(result.dayOfWeek).isEqualTo("화")
            assertThat(result.memberName).isEqualTo("최금주")
            assertThat(result.employeeCode).isEqualTo("20030117")
            assertThat(result.workingType).isNull()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(3)
            assertThat(result.reportProgress.workType).isEqualTo("진열")
            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts.all { !it.isRegistered }).isTrue()
        }

        @Test
        @DisplayName("성공 - 일정 없음")
        fun getDailySchedule_noSchedule_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns emptyList()
            every { displayWorkScheduleRepository.findByEmployeeAndStartDate(userId, date) } returns emptyList()

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isNull()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(0)
            assertThat(result.reportProgress.workType).isEmpty()
            assertThat(result.accounts).isEmpty()
        }

        @Test
        @DisplayName("성공 - 대휴 날짜 → workingType='대휴', accounts 빈 리스트")
        fun getDailySchedule_substituteHoliday() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2026, 4, 5)
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.ALT_HOLIDAY))

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isEqualTo("대휴")
            assertThat(result.accounts).isEmpty()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(0)
            assertThat(result.reportProgress.workType).isEmpty()
            verify(exactly = 0) { displayWorkScheduleRepository.findByEmployeeAndStartDate(any(), any()) }
        }

        @Test
        @DisplayName("성공 - 연차 날짜 → workingType='연차', accounts 빈 리스트")
        fun getDailySchedule_annualLeave() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2026, 4, 10)
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.ANNUAL_LEAVE))

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isEqualTo("연차")
            assertThat(result.accounts).isEmpty()
            assertThat(result.reportProgress.total).isEqualTo(0)
            verify(exactly = 0) { displayWorkScheduleRepository.findByEmployeeAndStartDate(any(), any()) }
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getDailySchedule_userNotFound() {
            // Given
            val userId = 999L
            val date = LocalDate.of(2020, 8, 4)

            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThrows<EmployeeNotFoundException> {
                myScheduleService.getDailySchedule(userId, date)
            }
        }
    }

    // ========== Helper Methods ==========

    private fun createMockEmployee(userId: Long, name: String, employeeCode: String, sfid: String? = null): Employee {
        return Employee(
            id = userId,
            employeeCode = employeeCode,
            password = "encoded",
            name = name,
            orgName = "서울지점",
            sfid = sfid
        )
    }

    private fun createMockMemberSchedule(
        employeeCode: String = "20030117",
        workingDate: LocalDate = LocalDate.now(),
        workingType: WorkingType = WorkingType.WORK
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            employee = Employee(id = 1L, employeeCode = employeeCode, name = "테스트"),
            workingDate = workingDate,
            workingType = workingType
        )
    }

    private fun createMockSchedule(
        id: Long = 0,
        typeOfWork1: TypeOfWork1? = TypeOfWork1.DISPLAY,
        startDate: LocalDate = LocalDate.now()
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            id = id,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
