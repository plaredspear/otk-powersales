package com.otoki.internal.repository

import com.otoki.internal.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface ScheduleRepository : JpaRepository<Schedule, Long> {

    /**
     * 사원 sfid와 근무 날짜로 일정 조회
     */
    fun findByEmployeeIdAndWorkingDate(employeeId: String, workingDate: LocalDate): List<Schedule>
}
