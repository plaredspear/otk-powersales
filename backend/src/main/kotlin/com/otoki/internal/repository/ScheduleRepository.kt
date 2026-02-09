package com.otoki.internal.repository

import com.otoki.internal.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface ScheduleRepository : JpaRepository<Schedule, Long> {

    /**
     * 사용자 ID와 일정 날짜로 일정 조회
     */
    fun findByUserIdAndScheduleDate(userId: Long, scheduleDate: LocalDate): List<Schedule>
}
