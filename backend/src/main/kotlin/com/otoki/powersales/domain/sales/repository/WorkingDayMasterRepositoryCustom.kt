package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import java.time.LocalDate

interface WorkingDayMasterRepositoryCustom {

    /**
     * `[start, end]` 구간의 영업일수 — `workingDateCheck = check` (영업일=1) row count.
     *
     * 레거시 SF SOQL `SELECT COUNT(Id) FROM WorkingDayMaster__c WHERE WorkingDate__c >= :start
     * AND WorkingDate__c <= :end AND WorkingDateCheck__c = 1` 정합. SF SOQL 이 IsDeleted=true 를
     * 자동 제외하는 것과 동등하게 soft-delete row(`isDeleted = true`)는 제외한다.
     */
    fun countWorkingDays(start: LocalDate, end: LocalDate, check: Double): Long

    /**
     * `[start, end]` 구간의 영업일 달력 row 목록 — 운영 관리 화면 조회용. soft-delete row 제외, 일자 오름차순.
     *
     * 화면이 생성자/수정자 이름을 표기하므로 `createdBy`/`lastModifiedBy` 를 함께 fetch (둘 다 `@ManyToOne`
     * 이라 Cartesian product 없음) — N+1 회피.
     */
    fun findByWorkingDateRange(start: LocalDate, end: LocalDate): List<WorkingDayMaster>
}
