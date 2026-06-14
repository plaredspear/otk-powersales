package com.otoki.powersales.domain.org.leave.dto

import com.otoki.powersales.domain.org.leave.entity.AlternativeHoliday
import java.time.LocalDate
import java.time.LocalDateTime

data class AlternativeHolidayCreateResponse(
    val id: Long,
    val actualWorkDate: LocalDate,
    val targetAltHolidayDate: LocalDate,
    val status: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: AlternativeHoliday): AlternativeHolidayCreateResponse =
            AlternativeHolidayCreateResponse(
                id = entity.id,
                actualWorkDate = entity.actualWorkDate,
                targetAltHolidayDate = entity.targetAltHolidayDate,
                status = entity.status.displayName,
                createdAt = entity.createdAt
            )
    }
}

data class AlternativeHolidayListItemResponse(
    val id: Long,
    val actualWorkDate: LocalDate,
    val targetAltHolidayDate: LocalDate,
    val confirmAltHolidayDate: LocalDate?,
    val status: String,
    val changeReason: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: AlternativeHoliday): AlternativeHolidayListItemResponse =
            AlternativeHolidayListItemResponse(
                id = entity.id,
                actualWorkDate = entity.actualWorkDate,
                targetAltHolidayDate = entity.targetAltHolidayDate,
                confirmAltHolidayDate = entity.confirmAltHolidayDate,
                status = entity.status.displayName,
                changeReason = entity.changeReason,
                createdAt = entity.createdAt
            )
    }
}
