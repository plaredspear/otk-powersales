package com.otoki.powersales.leave.dto

import com.otoki.powersales.leave.entity.AlternativeHoliday
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
                status = entity.status,
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
                status = entity.status,
                changeReason = entity.changeReason,
                createdAt = entity.createdAt
            )
    }
}
