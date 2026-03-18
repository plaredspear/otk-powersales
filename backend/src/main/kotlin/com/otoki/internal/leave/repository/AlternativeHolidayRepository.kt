package com.otoki.internal.leave.repository

import com.otoki.internal.leave.entity.AlternativeHoliday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AlternativeHolidayRepository : JpaRepository<AlternativeHoliday, Long>, AlternativeHolidayRepositoryCustom {

    fun existsByEmployeeIdAndActualWorkDateAndStatusNot(
        employeeId: String,
        actualWorkDate: LocalDate,
        status: String
    ): Boolean
}
