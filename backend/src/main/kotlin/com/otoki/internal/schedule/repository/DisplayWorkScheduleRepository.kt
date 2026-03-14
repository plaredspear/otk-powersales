package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DisplayWorkScheduleRepository : JpaRepository<DisplayWorkSchedule, Long>, DisplayWorkScheduleRepositoryCustom {

    /**
     * 사원(fullName sfid)의 특정 날짜 거래처 스케줄 조회
     */
    fun findByFullNameAndStartDate(fullName: String, startDate: LocalDate): List<DisplayWorkSchedule>

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 존재 여부
     */
    fun existsByFullNameAndAccountAndStartDate(fullName: String, account: String, startDate: LocalDate): Boolean

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 조회
     */
    fun findByFullNameAndAccountAndStartDate(fullName: String, account: String, startDate: LocalDate): DisplayWorkSchedule?

    // Phase2: 키워드 검색 — 삭제된 필드(storeName, storeCode, address) 참조로 비활성화
    // @Query("""
    //     SELECT s FROM DisplayWorkSchedule s
    //     WHERE s.fullName = :fullName
    //     AND s.startDate = :startDate
    //     AND (
    //         LOWER(s.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    //         OR LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
    //         OR LOWER(s.storeCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
    //     )
    // """)
    // fun findByFullNameAndStartDateAndKeyword(
    //     @Param("fullName") fullName: String,
    //     @Param("startDate") startDate: LocalDate,
    //     @Param("keyword") keyword: String
    // ): List<DisplayWorkSchedule>

    /**
     * 사원의 기간 내 스케줄 전체 조회
     */
    fun findByFullNameAndStartDateBetween(
        fullName: String,
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
     * 특정 월에 겹치는 확정 스케줄 조회 (특정 거래처 sfid 목록)
     * confirmed = true AND startDate <= monthEnd AND endDate >= monthStart AND account IN accountSfids
     */
    fun findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
        monthEnd: LocalDate,
        monthStart: LocalDate,
        accountSfids: List<String>
    ): List<DisplayWorkSchedule>
}
