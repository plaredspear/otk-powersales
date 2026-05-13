package com.otoki.powersales.leave.service

import com.otoki.powersales.common.entity.WorkingType
import com.otoki.powersales.leave.exception.*
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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

    fun validateWorkScheduleExists(employee: Employee, actualWorkDate: LocalDate) {
        if (!teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                employee, actualWorkDate, WorkingType.WORK
            )
        ) {
            throw AltHolidayNoWorkScheduleException()
        }
    }

    fun validateNoDuplicate(employeeId: Long, actualWorkDate: LocalDate) {
        if (alternativeHolidayRepository.existsByEmployeeIdAndActualWorkDateAndStatusNot(
                employeeId, actualWorkDate, com.otoki.powersales.leave.entity.AltHolidayStatus.REJECTED
            )
        ) {
            throw AltHolidayDuplicateException()
        }
    }
}
