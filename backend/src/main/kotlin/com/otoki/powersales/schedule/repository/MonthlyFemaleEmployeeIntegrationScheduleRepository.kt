package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyFemaleEmployeeIntegrationScheduleRepository : JpaRepository<MonthlyFemaleEmployeeIntegrationSchedule, Long> {

    fun findByEmployeeIdAndAccountIdAndYearAndMonth(
        employeeId: Long,
        accountId: Int,
        year: String,
        month: String
    ): MonthlyFemaleEmployeeIntegrationSchedule?
}
