package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyFemaleEmployeeIntegrationScheduleRepository :
    JpaRepository<MonthlyFemaleEmployeeIntegrationSchedule, Long> {

    fun findByEmployeeIdAndAccountIdAndYearAndMonth(
        employeeId: Long,
        accountId: Int,
        year: String,
        month: String
    ): MonthlyFemaleEmployeeIntegrationSchedule?

    /**
     * MfeisThisMonthRevenueBatch 추출용 — 전월 + 상시 카테고리 row.
     *
     * legacy `UpdateThisMonthRevenueBatch.start` (`cls:7-18`) 의
     * `WHERE WorkingCategory5__c LIKE '%상시%' AND Year_Month__c = lastYearMonth` 동등.
     */
    fun findByYearAndMonthAndWorkingCategory5Containing(
        year: String,
        month: String,
        workingCategory5: String,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>

    /**
     * spec #680 §5.3 — `accountConvertedHeadcount` 합산용.
     *
     * legacy `setAccountConvertedHeadcount` 의 `convertedCntMap` 동등 — 거래처+근무유형1+년월
     * 단위 (사원 무관) MFEIS row 의 환산인원 (convertedHeadcount) 을 합산하기 위해
     * 같은 그룹 row 들을 모두 조회.
     */
    fun findByAccountIdAndWorkingCategory1AndYearAndMonth(
        accountId: Int,
        workingCategory1: String,
        year: String,
        month: String,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>
}
