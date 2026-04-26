package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DisplayWorkScheduleRepository : JpaRepository<DisplayWorkSchedule, Long>, DisplayWorkScheduleRepositoryCustom {

    /**
     * 특정 월에 겹치는 확정 스케줄 조회 (전체)
     * confirmed = true AND startDate <= monthEnd AND endDate >= monthStart
     */
    fun findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        monthEnd: LocalDate,
        monthStart: LocalDate
    ): List<DisplayWorkSchedule>

}
