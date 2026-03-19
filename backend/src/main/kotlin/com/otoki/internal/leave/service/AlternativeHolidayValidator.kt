package com.otoki.internal.leave.service

import com.otoki.internal.leave.exception.*
import com.otoki.internal.leave.repository.AlternativeHolidayRepository
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class AlternativeHolidayValidator(
    private val holidayMasterService: HolidayMasterService,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val alternativeHolidayRepository: AlternativeHolidayRepository
) {

    fun validateConfirmDate(date: LocalDate) {
        if (holidayMasterService.isHoliday(date)) {
            throw AltHolidayConfirmDateIsHolidayException()
        }
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            throw AltHolidayConfirmDateIsWeekendException()
        }
    }

    fun validateActualWorkDate(date: LocalDate) {
        val isWeekday = date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        val isHoliday = holidayMasterService.isHoliday(date)
        if (isWeekday && !isHoliday) {
            throw AltHolidayActualDateIsWeekdayException()
        }
    }

    fun validateWorkScheduleExists(employeeId: Long, actualWorkDate: LocalDate) {
        if (!teamMemberScheduleRepository.existsByEmployeeIdAndWorkingDateAndWorkingType(
                employeeId, actualWorkDate, "근무"
            )
        ) {
            throw AltHolidayNoWorkScheduleException()
        }
    }

    fun validateNoDuplicate(employeeId: Long, actualWorkDate: LocalDate) {
        if (alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(
                employeeId, actualWorkDate, "반려"
            )
        ) {
            throw AltHolidayDuplicateException()
        }
    }
}
