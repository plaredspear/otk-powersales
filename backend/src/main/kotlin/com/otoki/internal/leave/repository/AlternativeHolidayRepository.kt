package com.otoki.internal.leave.repository

import com.otoki.internal.leave.entity.AlternativeHoliday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AlternativeHolidayRepository : JpaRepository<AlternativeHoliday, Long>, AlternativeHolidayRepositoryCustom {

    fun existsByEmployeeNumberAndActualWorkDateAndStatusNot(
        employeeNumber: String,
        actualWorkDate: LocalDate,
        status: String
    ): Boolean

    fun findByEmployeeNumberAndActualWorkDateBetweenOrderByCreatedAtDesc(
        employeeNumber: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<AlternativeHoliday>
}
