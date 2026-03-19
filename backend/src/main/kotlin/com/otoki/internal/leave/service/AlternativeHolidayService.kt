package com.otoki.internal.leave.service

import com.otoki.internal.leave.dto.AlternativeHolidayCreateResponse
import com.otoki.internal.leave.dto.AlternativeHolidayListItemResponse
import com.otoki.internal.leave.entity.AlternativeHoliday
import com.otoki.internal.leave.exception.EmployeeNotFoundException
import com.otoki.internal.leave.repository.AlternativeHolidayRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AlternativeHolidayService(
    private val alternativeHolidayRepository: AlternativeHolidayRepository,
    private val userRepository: UserRepository,
    private val validator: AlternativeHolidayValidator
) {

    @Transactional
    fun createAlternativeHoliday(
        userId: Long,
        actualWorkDate: LocalDate,
        targetAltHolidayDate: LocalDate
    ): AlternativeHolidayCreateResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        validator.validateConfirmDate(targetAltHolidayDate)
        validator.validateActualWorkDate(actualWorkDate)
        validator.validateWorkScheduleExists(user.id, actualWorkDate)
        validator.validateNoDuplicate(user.id, actualWorkDate)

        val altHoliday = alternativeHolidayRepository.save(
            AlternativeHoliday(
                employeeId = user.id,
                employeeName = user.name,
                actualWorkDate = actualWorkDate,
                targetAltHolidayDate = targetAltHolidayDate,
                status = "신규",
                createdBy = user.employeeNumber
            )
        )

        return AlternativeHolidayCreateResponse.from(altHoliday)
    }

    fun getAlternativeHolidays(
        userId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<AlternativeHolidayListItemResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val effectiveEndDate = endDate ?: LocalDate.now()
        val effectiveStartDate = startDate ?: effectiveEndDate.minusMonths(3)

        return alternativeHolidayRepository
            .findByEmployeeIdAndActualWorkDateBetweenOrderByCreatedAtDesc(
                user.id, effectiveStartDate, effectiveEndDate
            )
            .map { AlternativeHolidayListItemResponse.from(it) }
    }
}
