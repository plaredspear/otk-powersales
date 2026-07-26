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

    /**
     * spec #680 §5.3 — `accountConvertedHeadcount` 합산의 배치 버전.
     *
     * `findByAccountIdAndWorkingCategory1AndYearAndMonth` 를 그룹 루프 안에서 accountId 별로
     * 반복 호출하면 출근등록이 월말로 갈수록 그룹 수만큼 쿼리가 늘어난다. 재집계 대상 거래처
     * 전체를 accountId IN 으로 1회 조회한 뒤, 호출측에서 (accountId, workingCategory1) 로
     * 그룹핑해 map lookup 으로 사용한다. 년월 필터는 상수라 그대로 동일.
     */
    fun findByAccountIdInAndYearAndMonth(
        accountIds: Collection<Long>,
        year: String,
        month: String,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>
}
