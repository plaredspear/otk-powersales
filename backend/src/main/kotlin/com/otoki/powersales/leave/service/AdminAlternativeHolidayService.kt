package com.otoki.powersales.leave.service

import com.otoki.powersales.common.entity.WorkingType
import com.otoki.powersales.leave.dto.request.AlternativeHolidayApproveRequest
import com.otoki.powersales.leave.dto.request.AlternativeHolidayCreateRequest
import com.otoki.powersales.leave.dto.request.AlternativeHolidayRejectRequest
import com.otoki.powersales.leave.dto.response.AlternativeHolidayApproveResponse
import com.otoki.powersales.leave.dto.response.AlternativeHolidayCreateResponse
import com.otoki.powersales.leave.dto.response.AlternativeHolidayListItem
import com.otoki.powersales.leave.dto.response.AlternativeHolidayRejectResponse
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.exception.AltHolidayInvalidStatusException
import com.otoki.powersales.leave.exception.AltHolidayNotFoundException
import com.otoki.powersales.leave.exception.ChangeReasonRequiredException
import com.otoki.powersales.leave.exception.EmployeeNotFoundException
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminAlternativeHolidayService(
    private val alternativeHolidayRepository: AlternativeHolidayRepository,
    private val employeeRepository: EmployeeRepository,
    private val validator: AlternativeHolidayValidator,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    fun getAlternativeHolidays(
        startDate: LocalDate,
        endDate: LocalDate,
        status: String?,
        employeeCode: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem> {
        return alternativeHolidayRepository.findByFilters(startDate, endDate, status, employeeCode, orgCode)
    }

    @Transactional
    fun createAlternativeHoliday(
        request: AlternativeHolidayCreateRequest,
        adminUserId: Long
    ): AlternativeHolidayCreateResponse {
        val employee = employeeRepository.findByEmployeeCode(request.employeeCode)
            .orElseThrow { EmployeeNotFoundException() }

        val admin = employeeRepository.findById(adminUserId)
            .orElseThrow { EmployeeNotFoundException() }

        validator.validateConfirmDate(request.targetAltHolidayDate)
        validator.validateActualWorkDate(request.actualWorkDate)
        validator.validateWorkScheduleExists(employee, request.actualWorkDate)
        validator.validateNoDuplicate(employee.id, request.actualWorkDate)

        val altHoliday = alternativeHolidayRepository.save(
            AlternativeHoliday(
                employeeId = employee.id,
                actualWorkDate = request.actualWorkDate,
                targetAltHolidayDate = request.targetAltHolidayDate,
                status = com.otoki.powersales.leave.entity.AltHolidayStatus.NEW,
                createdByEmpNo = admin.employeeCode
            )
        )

        return AlternativeHolidayCreateResponse.Companion.from(altHoliday)
    }

    @Transactional
    fun approveAlternativeHoliday(id: Long, request: AlternativeHolidayApproveRequest): AlternativeHolidayApproveResponse {
        val altHoliday = findById(id)

        if (!altHoliday.canTransition()) {
            throw AltHolidayInvalidStatusException()
        }

        val confirmDate = request.confirmAltHolidayDate ?: altHoliday.targetAltHolidayDate
        validator.validateConfirmDate(confirmDate)

        val changeReason = if (request.confirmAltHolidayDate != null &&
            request.confirmAltHolidayDate != altHoliday.targetAltHolidayDate) {
            "관리자 조정"
        } else {
            null
        }

        altHoliday.approve(confirmDate, changeReason)

        val employee = employeeRepository.findById(altHoliday.employeeId)
            .orElseThrow { IllegalStateException("Employee not found: ${altHoliday.employeeId}") }

        teamMemberScheduleRepository.save(
            TeamMemberSchedule(
                employee = employee,
                workingDate = confirmDate,
                workingType = WorkingType.ALT_HOLIDAY,
                altHoliday = altHoliday
            )
        )

        return AlternativeHolidayApproveResponse.Companion.from(altHoliday)
    }

    @Transactional
    fun rejectAlternativeHoliday(id: Long, request: AlternativeHolidayRejectRequest): AlternativeHolidayRejectResponse {
        val altHoliday = findById(id)

        if (!altHoliday.canTransition()) {
            throw AltHolidayInvalidStatusException()
        }

        if (request.changeReason.isBlank()) {
            throw ChangeReasonRequiredException()
        }

        altHoliday.reject(request.changeReason)

        return AlternativeHolidayRejectResponse.Companion.from(altHoliday)
    }

    private fun findById(id: Long): AlternativeHoliday =
        alternativeHolidayRepository.findById(id).orElseThrow { AltHolidayNotFoundException() }

}
