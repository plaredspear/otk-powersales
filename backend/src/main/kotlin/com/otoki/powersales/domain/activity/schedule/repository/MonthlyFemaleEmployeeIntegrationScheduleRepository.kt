package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyFemaleEmployeeIntegrationScheduleRepository :
    JpaRepository<MonthlyFemaleEmployeeIntegrationSchedule, Long>,
    MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    /**
     * 사원+거래처+년월 MFEIS row 목록 — 레거시 집계 키(ExternalKey) 가 근무유형 조합·costCenter 까지
     * 포함하므로 같은 사원×거래처×년월에 복수 row 가 존재할 수 있다 (List 반환).
     */
    fun findByEmployeeIdAndAccountIdAndYearAndMonth(
        employeeId: Long,
        accountId: Long,
        year: String,
        month: String
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>

    /**
     * 사원+년월 MFEIS row 전건 — `refreshIntegration` 의 재집계 대상(기존 row upsert 매칭 + stale 키 삭제)용.
     */
    fun findByEmployeeIdAndYearAndMonth(
        employeeId: Long,
        year: String,
        month: String
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>

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
        accountId: Long,
        workingCategory1: String,
        year: String,
        month: String,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>
}
