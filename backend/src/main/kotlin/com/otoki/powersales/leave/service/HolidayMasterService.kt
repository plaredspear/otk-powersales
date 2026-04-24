package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.repository.HolidayMasterRepository
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
