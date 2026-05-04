package com.otoki.powersales.leave.dto.response

import com.otoki.powersales.leave.entity.HolidayMaster
import java.time.LocalDate

data class HolidayMasterResponse(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
    val type: String
) {
    companion object {
        fun from(entity: HolidayMaster): HolidayMasterResponse = HolidayMasterResponse(
            id = entity.id,
            holidayDate = entity.holidayDate,
            name = entity.name,
            type = entity.type
        )
    }
}
