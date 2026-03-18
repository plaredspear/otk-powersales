package com.otoki.internal.leave.service

import com.otoki.internal.leave.entity.HolidayMaster
import com.otoki.internal.leave.repository.HolidayMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class HolidayMasterService(
    private val holidayMasterRepository: HolidayMasterRepository
) {

    fun isHoliday(date: LocalDate): Boolean {
        return holidayMasterRepository.existsByHolidayDate(date)
    }

    fun getHolidaysByDateRange(startDate: LocalDate, endDate: LocalDate): List<HolidayMaster> {
        return holidayMasterRepository.findByHolidayDateBetween(startDate, endDate)
    }
}
