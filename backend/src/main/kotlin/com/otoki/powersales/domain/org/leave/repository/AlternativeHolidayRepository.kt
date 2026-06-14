package com.otoki.powersales.domain.org.leave.repository

import com.otoki.powersales.domain.org.leave.enums.AltHolidayStatus
import com.otoki.powersales.domain.org.leave.entity.AlternativeHoliday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AlternativeHolidayRepository : JpaRepository<AlternativeHoliday, Long>, AlternativeHolidayRepositoryCustom {

    fun existsByEmployeeIdAndActualWorkDateAndStatusNot(
        employeeId: Long,
        actualWorkDate: LocalDate,
        status: AltHolidayStatus
    ): Boolean

    fun findByEmployeeIdAndActualWorkDateBetweenOrderByCreatedAtDesc(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<AlternativeHoliday>
}
