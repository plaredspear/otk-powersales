package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyFemaleEmployeeIntegrationScheduleRepository : JpaRepository<MonthlyFemaleEmployeeIntegrationSchedule, Long> {

    fun findByEmployeeIdAndAccountIdAndYearAndMonth(
        employeeId: Long,
        accountId: Int,
        year: String,
        month: String
    ): MonthlyFemaleEmployeeIntegrationSchedule?
}
