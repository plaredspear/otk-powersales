package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

/**
 * 영업일관리마스터 Repository (SF `WorkingDayMaster__c`).
 *
 * 월매출 현황 "기준 진도율" 산출용 영업일수 count 를 제공한다.
 */
interface WorkingDayMasterRepository : JpaRepository<WorkingDayMaster, Long> {

    /**
     * `[start, end]` 구간의 영업일수 — `workingDateCheck = check` (영업일=1) row count.
     *
     * 레거시 SF SOQL `SELECT COUNT(Id) FROM WorkingDayMaster__c WHERE WorkingDate__c >= :start
     * AND WorkingDate__c <= :end AND WorkingDateCheck__c = 1` 정합. SF SOQL 이 IsDeleted=true 를
     * 자동 제외하는 것과 동등하게 soft-delete row(`isDeleted = true`)는 제외한다.
     */
    @Query(
        """
        SELECT COUNT(w) FROM WorkingDayMaster w
        WHERE w.workingDate >= :start
          AND w.workingDate <= :end
          AND w.workingDateCheck = :check
          AND (w.isDeleted IS NULL OR w.isDeleted = false)
        """
    )
    fun countWorkingDays(
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
        @Param("check") check: Double,
    ): Long

    /**
     * `[start, end]` 구간의 영업일 달력 row 목록 — 운영 관리 화면 조회용. soft-delete row 제외, 일자 오름차순.
     *
     * 화면이 생성자/수정자 이름을 표기하므로 `createdBy`/`lastModifiedBy` 를 함께 fetch (둘 다 `@ManyToOne`
     * 이라 Cartesian product 없음) — N+1 회피.
     */
    @Query(
        """
        SELECT w FROM WorkingDayMaster w
        LEFT JOIN FETCH w.createdBy
        LEFT JOIN FETCH w.lastModifiedBy
        WHERE w.workingDate >= :start
          AND w.workingDate <= :end
          AND (w.isDeleted IS NULL OR w.isDeleted = false)
        ORDER BY w.workingDate ASC
        """
    )
    fun findByWorkingDateRange(
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
    ): List<WorkingDayMaster>
}
