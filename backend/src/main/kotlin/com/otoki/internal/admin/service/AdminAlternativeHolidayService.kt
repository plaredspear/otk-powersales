package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.AlternativeHolidayApproveRequest
import com.otoki.internal.admin.dto.request.AlternativeHolidayCreateRequest
import com.otoki.internal.admin.dto.request.AlternativeHolidayRejectRequest
import com.otoki.internal.admin.dto.response.AlternativeHolidayApproveResponse
import com.otoki.internal.admin.dto.response.AlternativeHolidayCreateResponse
import com.otoki.internal.admin.dto.response.AlternativeHolidayListItem
import com.otoki.internal.admin.dto.response.AlternativeHolidayRejectResponse
import com.otoki.internal.leave.entity.AlternativeHoliday
import com.otoki.internal.leave.exception.*
import com.otoki.internal.leave.repository.AlternativeHolidayRepository
import com.otoki.internal.leave.service.AlternativeHolidayValidator
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminAlternativeHolidayService(
    private val alternativeHolidayRepository: AlternativeHolidayRepository,
    private val userRepository: UserRepository,
    private val validator: AlternativeHolidayValidator,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    fun getAlternativeHolidays(
        startDate: LocalDate,
        endDate: LocalDate,
        status: String?,
        employeeNumber: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem> {
        return alternativeHolidayRepository.findByFilters(startDate, endDate, status, employeeNumber, orgCode)
    }

    @Transactional
    fun createAlternativeHoliday(
        request: AlternativeHolidayCreateRequest,
        adminUserId: Long
    ): AlternativeHolidayCreateResponse {
        val employee = userRepository.findByEmployeeNumber(request.employeeNumber)
            .orElseThrow { com.otoki.internal.leave.exception.EmployeeNotFoundException() }

        val admin = userRepository.findById(adminUserId)
            .orElseThrow { com.otoki.internal.leave.exception.EmployeeNotFoundException() }

        validator.validateConfirmDate(request.targetAltHolidayDate)
        validator.validateActualWorkDate(request.actualWorkDate)
        validator.validateWorkScheduleExists(employee.id, request.actualWorkDate)
        validator.validateNoDuplicate(employee.id, request.actualWorkDate)

        val altHoliday = alternativeHolidayRepository.save(
            AlternativeHoliday(
                employeeId = employee.id,
                employeeName = employee.name,
                actualWorkDate = request.actualWorkDate,
                targetAltHolidayDate = request.targetAltHolidayDate,
                status = "신규",
                createdBy = admin.employeeNumber
            )
        )

        return AlternativeHolidayCreateResponse.from(altHoliday)
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

        teamMemberScheduleRepository.save(
            TeamMemberSchedule(
                employeeId = altHoliday.employeeId,
                workingDate = confirmDate,
                workingType = "대휴",
                altHolidayId = altHoliday.id
            )
        )

        return AlternativeHolidayApproveResponse.from(altHoliday)
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

        return AlternativeHolidayRejectResponse.from(altHoliday)
    }

    private fun findById(id: Long): AlternativeHoliday =
        alternativeHolidayRepository.findById(id).orElseThrow { AltHolidayNotFoundException() }

}
