package com.otoki.powersales.leave.repository

import com.otoki.powersales.admin.dto.response.AlternativeHolidayListItem
import java.time.LocalDate

interface AlternativeHolidayRepositoryCustom {

    fun findByFilters(
        startDate: LocalDate,
        endDate: LocalDate,
        status: String?,
        employeeCode: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem>
}
