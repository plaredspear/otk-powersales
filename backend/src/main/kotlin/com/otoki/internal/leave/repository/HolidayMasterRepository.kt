package com.otoki.internal.leave.repository

import com.otoki.internal.leave.entity.HolidayMaster
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface HolidayMasterRepository : JpaRepository<HolidayMaster, Long> {

    fun findByYearOrderByHolidayDateAsc(year: Int): List<HolidayMaster>

    fun existsByHolidayDate(holidayDate: LocalDate): Boolean

    fun existsByHolidayDateAndIdNot(holidayDate: LocalDate, id: Long): Boolean

    fun findByHolidayDate(holidayDate: LocalDate): HolidayMaster?

    fun findByHolidayDateBetween(startDate: LocalDate, endDate: LocalDate): List<HolidayMaster>
}
