package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DisplayWorkScheduleRepository : JpaRepository<DisplayWorkSchedule, Long>, DisplayWorkScheduleRepositoryCustom {

    /**
     * 사원(employeeId PK)의 특정 날짜 거래처 스케줄 조회
     */
    fun findByEmployeeIdAndStartDate(employeeId: Long, startDate: LocalDate): List<DisplayWorkSchedule>

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 존재 여부
     */
    fun existsByEmployeeIdAndAccountIdAndStartDate(employeeId: Long, accountId: Int, startDate: LocalDate): Boolean

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 조회
     */
    fun findByEmployeeIdAndAccountIdAndStartDate(employeeId: Long, accountId: Int, startDate: LocalDate): DisplayWorkSchedule?

    /**
     * 사원의 기간 내 스케줄 전체 조회
     */
    fun findByEmployeeIdAndStartDateBetween(
        employeeId: Long,
        startDateStart: LocalDate,
        startDateEnd: LocalDate
    ): List<DisplayWorkSchedule>

    /**
     * 특정 월에 겹치는 확정 스케줄 조회 (전체)
     * confirmed = true AND startDate <= monthEnd AND endDate >= monthStart
     */
    fun findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        monthEnd: LocalDate,
        monthStart: LocalDate
    ): List<DisplayWorkSchedule>

    /**
     * 특정 월에 겹치는 확정 스케줄 조회 (특정 거래처 ID 목록)
     * confirmed = true AND startDate <= monthEnd AND endDate >= monthStart AND accountId IN accountIds
     */
    fun findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIdIn(
        monthEnd: LocalDate,
        monthStart: LocalDate,
        accountIds: List<Int>
    ): List<DisplayWorkSchedule>

}
