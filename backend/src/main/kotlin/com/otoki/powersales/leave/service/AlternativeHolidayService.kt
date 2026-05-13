package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.dto.AlternativeHolidayCreateResponse
import com.otoki.powersales.leave.dto.AlternativeHolidayListItemResponse
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.exception.EmployeeNotFoundException
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AlternativeHolidayService(
    private val alternativeHolidayRepository: AlternativeHolidayRepository,
    private val employeeRepository: EmployeeRepository,
    private val validator: AlternativeHolidayValidator
) {

    @Transactional
    fun createAlternativeHoliday(
        userId: Long,
        actualWorkDate: LocalDate,
        targetAltHolidayDate: LocalDate
    ): AlternativeHolidayCreateResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        validator.validateConfirmDate(targetAltHolidayDate)
        validator.validateActualWorkDate(actualWorkDate)
        validator.validateWorkScheduleExists(employee, actualWorkDate)
        validator.validateNoDuplicate(employee.id, actualWorkDate)

        val altHoliday = alternativeHolidayRepository.save(
            AlternativeHoliday(
                employeeId = employee.id,
                actualWorkDate = actualWorkDate,
                targetAltHolidayDate = targetAltHolidayDate,
                status = com.otoki.powersales.leave.entity.AltHolidayStatus.NEW,
                createdBy = employee.employeeCode
            )
        )

        return AlternativeHolidayCreateResponse.from(altHoliday)
    }

    fun getAlternativeHolidays(
        userId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<AlternativeHolidayListItemResponse> {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val effectiveEndDate = endDate ?: LocalDate.now()
        val effectiveStartDate = startDate ?: effectiveEndDate.minusMonths(3)

        return alternativeHolidayRepository
            .findByEmployeeIdAndActualWorkDateBetweenOrderByCreatedAtDesc(
                employee.id, effectiveStartDate, effectiveEndDate
            )
            .map { AlternativeHolidayListItemResponse.from(it) }
    }
}
