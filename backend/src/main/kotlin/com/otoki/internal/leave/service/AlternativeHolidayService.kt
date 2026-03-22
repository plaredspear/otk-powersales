package com.otoki.internal.leave.service

import com.otoki.internal.leave.dto.AlternativeHolidayCreateResponse
import com.otoki.internal.leave.dto.AlternativeHolidayListItemResponse
import com.otoki.internal.leave.entity.AlternativeHoliday
import com.otoki.internal.leave.exception.EmployeeNotFoundException
import com.otoki.internal.leave.repository.AlternativeHolidayRepository
import com.otoki.internal.sap.repository.EmployeeRepository
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
        validator.validateWorkScheduleExists(employee.id, actualWorkDate)
        validator.validateNoDuplicate(employee.id, actualWorkDate)

        val altHoliday = alternativeHolidayRepository.save(
            AlternativeHoliday(
                employeeId = employee.id,
                employeeName = employee.name,
                actualWorkDate = actualWorkDate,
                targetAltHolidayDate = targetAltHolidayDate,
                status = "신규",
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
