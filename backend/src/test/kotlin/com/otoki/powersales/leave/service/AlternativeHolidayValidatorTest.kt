package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.exception.*
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThatCode
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

@ExtendWith(MockitoExtension::class)
@DisplayName("AlternativeHolidayValidator 테스트")
class AlternativeHolidayValidatorTest {

    @Mock private lateinit var holidayMasterService: HolidayMasterService
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @Mock private lateinit var alternativeHolidayRepository: AlternativeHolidayRepository
    @InjectMocks private lateinit var validator: AlternativeHolidayValidator

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
            whenever(holidayMasterService.isHoliday(monday)).thenReturn(false)
            assertThatCode { validator.validateConfirmDate(monday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun holiday() {
            whenever(holidayMasterService.isHoliday(monday)).thenReturn(true)
            assertThatThrownBy { validator.validateConfirmDate(monday) }
                .isInstanceOf(AltHolidayConfirmDateIsHolidayException::class.java)
        }

        @Test
        @DisplayName("토요일 -> AltHolidayConfirmDateIsWeekendException")
        fun saturdayFails() {
            whenever(holidayMasterService.isHoliday(saturday)).thenReturn(false)
            assertThatThrownBy { validator.validateConfirmDate(saturday) }
                .isInstanceOf(AltHolidayConfirmDateIsWeekendException::class.java)
        }

        @Test
        @DisplayName("일요일 -> AltHolidayConfirmDateIsWeekendException")
        fun sundayFails() {
            whenever(holidayMasterService.isHoliday(sunday)).thenReturn(false)
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
            assertThatCode { validator.validateActualWorkDate(saturday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("공휴일(평일) -> 통과")
        fun weekdayHoliday_passes() {
            whenever(holidayMasterService.isHoliday(wednesday)).thenReturn(true)
            assertThatCode { validator.validateActualWorkDate(wednesday) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("평일 + 비공휴일 -> AltHolidayActualDateIsWeekdayException")
        fun weekday_nonHoliday_fails() {
            whenever(holidayMasterService.isHoliday(wednesday)).thenReturn(false)
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
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(true)
            assertThatCode { validator.validateWorkScheduleExists(testEmployee, saturday) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("근무 스케줄 없음 -> AltHolidayNoWorkScheduleException")
        fun noSchedule() {
            whenever(teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                any(), any(), any()
            )).thenReturn(false)
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
            whenever(alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(
                1L, saturday, "반려"
            )).thenReturn(false)
            assertThatCode { validator.validateNoDuplicate(1L, saturday) }
                .doesNotThrowAnyException()
        }

        @Test
        @DisplayName("중복 존재 -> AltHolidayDuplicateException")
        fun duplicate() {
            whenever(alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(
                1L, saturday, "반려"
            )).thenReturn(true)
            assertThatThrownBy { validator.validateNoDuplicate(1L, saturday) }
                .isInstanceOf(AltHolidayDuplicateException::class.java)
        }
    }
}
