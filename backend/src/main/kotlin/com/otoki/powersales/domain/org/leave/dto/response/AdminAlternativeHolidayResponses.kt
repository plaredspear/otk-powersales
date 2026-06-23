package com.otoki.powersales.domain.org.leave.dto.response

import com.otoki.powersales.domain.org.leave.enums.AltHolidayStatus
import com.otoki.powersales.domain.org.leave.entity.AlternativeHoliday
import java.time.LocalDate
import java.time.LocalDateTime

data class AlternativeHolidayListItem(
    val id: Long,
    val employeeCode: String,
    val employeeName: String,
    val orgName: String?,
    val actualWorkDate: LocalDate?,
    val targetAltHolidayDate: LocalDate?,
    val confirmAltHolidayDate: LocalDate?,
    val status: AltHolidayStatus?,
    val changeReason: String?,
    val createdByEmpNo: String,
    val createdAt: LocalDateTime
)

data class AlternativeHolidayCreateResponse(
    val id: Long,
    val status: String?
) {
    companion object {
        fun from(entity: AlternativeHoliday): AlternativeHolidayCreateResponse =
            AlternativeHolidayCreateResponse(
                id = entity.id,
                status = entity.status?.displayName
            )
    }
}

data class AlternativeHolidayApproveResponse(
    val id: Long,
    val status: String?,
    val confirmAltHolidayDate: LocalDate?
) {
    companion object {
        fun from(entity: AlternativeHoliday): AlternativeHolidayApproveResponse =
            AlternativeHolidayApproveResponse(
                id = entity.id,
                status = entity.status?.displayName,
                confirmAltHolidayDate = entity.confirmAltHolidayDate
            )
    }
}

data class AlternativeHolidayRejectResponse(
    val id: Long,
    val status: String?
) {
    companion object {
        fun from(entity: AlternativeHoliday): AlternativeHolidayRejectResponse =
            AlternativeHolidayRejectResponse(
                id = entity.id,
                status = entity.status?.displayName
            )
    }
}
