package com.otoki.internal.leave.repository

import com.otoki.internal.admin.dto.response.AlternativeHolidayListItem
import java.time.LocalDate

interface AlternativeHolidayRepositoryCustom {

    fun findByFilters(
        startDate: LocalDate,
        endDate: LocalDate,
        status: String?,
        employeeNumber: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem>
}
