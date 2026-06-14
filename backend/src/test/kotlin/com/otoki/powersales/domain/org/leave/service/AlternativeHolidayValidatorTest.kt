package com.otoki.powersales.domain.org.leave.service

import com.otoki.powersales.domain.org.leave.exception.*
import com.otoki.powersales.domain.org.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.leave.enums.AltHolidayStatus
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AlternativeHolidayValidator 테스트")
class AlternativeHolidayValidatorTest {

    private val holidayMasterService: HolidayMasterService = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val alternativeHolidayRepository: AlternativeHolidayRepository = mockk()

    private val validator = AlternativeHolidayValidator(
        holidayMasterService,
        teamMemberScheduleRepository,
        alternativeHolidayRepository,
    )

    // 2026-03-07 = 토요일, 2026-03-08 = 일요일, 2026-03-09 = 월요일, 2026-03-11 = 수요일
    private val saturday = LocalDate.of(2026, 3, 7)
    private val sunday = LocalDate.of(2026, 3, 8)
    private val monday = LocalDate.of(2026, 3, 9)
    private val wednesday = LocalDate.of(2026, 3, 11)

    @Nested
    @DisplayName("validateConfirmDate - 신청일 검증")
    inner class ValidateConfirmDateTests {

        @Test
        @DisplayName("평일 + 비공휴일 -> 통과")
        fun validWeekday() {
            every { holidayMasterService.isHoliday(monday) } returns false
            assertThatCode { validator.validateConfirmDate(monday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun holiday() {
            every { holidayMasterService.isHoliday(monday) } returns true
            assertThatThrownBy { validator.validateConfirmDate(monday) }
                .isInstanceOf(AltHolidayConfirmDateIsHolidayException::class.java)
        }

        @Test
        @DisplayName("토요일 -> AltHolidayConfirmDateIsWeekendException")
        fun saturdayFails() {
            every { holidayMasterService.isHoliday(saturday) } returns false
            assertThatThrownBy { validator.validateConfirmDate(saturday) }
                .isInstanceOf(AltHolidayConfirmDateIsWeekendException::class.java)
        }

        @Test
        @DisplayName("일요일 -> AltHolidayConfirmDateIsWeekendException")
        fun sundayFails() {
            every { holidayMasterService.isHoliday(sunday) } returns false
            assertThatThrownBy { validator.validateConfirmDate(sunday) }
                .isInstanceOf(AltHolidayConfirmDateIsWeekendException::class.java)
        }
    }

    @Nested
    @DisplayName("validateActualWorkDate - 대상일 검증")
    inner class ValidateActualWorkDateTests {

        @Test
        @DisplayName("토요일 -> 통과")
        fun saturday_passes() {
            every { holidayMasterService.isHoliday(saturday) } returns false
            assertThatCode { validator.validateActualWorkDate(saturday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("공휴일(평일) -> 통과")
        fun weekdayHoliday_passes() {
            every { holidayMasterService.isHoliday(wednesday) } returns true
            assertThatCode { validator.validateActualWorkDate(wednesday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("평일 + 비공휴일 -> AltHolidayActualDateIsWeekdayException")
        fun weekday_nonHoliday_fails() {
            every { holidayMasterService.isHoliday(wednesday) } returns false
            assertThatThrownBy { validator.validateActualWorkDate(wednesday) }
                .isInstanceOf(AltHolidayActualDateIsWeekdayException::class.java)
        }
    }

    @Nested
    @DisplayName("validateWorkScheduleExists - 근무 스케줄 확인")
    inner class ValidateWorkScheduleTests {

        private val testEmployee = Employee(id = 1L, employeeCode = "EMP001", name = "테스트")

        @Test
        @DisplayName("근무 스케줄 존재 -> 통과")
        fun scheduleExists() {
            every { teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any()) } returns true
            assertThatCode { validator.validateWorkScheduleExists(testEmployee, saturday) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("근무 스케줄 없음 -> AltHolidayNoWorkScheduleException")
        fun noSchedule() {
            every { teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any()) } returns false
            assertThatThrownBy { validator.validateWorkScheduleExists(testEmployee, saturday) }
                .isInstanceOf(AltHolidayNoWorkScheduleException::class.java)
        }
    }

    @Nested
    @DisplayName("validateNoDuplicate - 중복 신청 확인")
    inner class ValidateNoDuplicateTests {

        @Test
        @DisplayName("중복 없음 -> 통과")
        fun noDuplicate() {
            every {
                alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(1L, saturday, AltHolidayStatus.REJECTED)
            } returns false
            assertThatCode { validator.validateNoDuplicate(1L, saturday) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("중복 존재 -> AltHolidayDuplicateException")
        fun duplicate() {
            every {
                alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(1L, saturday, AltHolidayStatus.REJECTED)
            } returns true
            assertThatThrownBy { validator.validateNoDuplicate(1L, saturday) }
                .isInstanceOf(AltHolidayDuplicateException::class.java)
        }
    }
}
