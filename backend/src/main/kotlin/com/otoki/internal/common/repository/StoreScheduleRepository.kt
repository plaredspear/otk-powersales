package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.StoreSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface StoreScheduleRepository : JpaRepository<StoreSchedule, Long> {

    /**
     * 사원(fullName sfid)의 특정 날짜 거래처 스케줄 조회
     */
    fun findByFullNameAndStartDate(fullName: String, startDate: LocalDate): List<StoreSchedule>

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 존재 여부
     */
    fun existsByFullNameAndAccountAndStartDate(fullName: String, account: String, startDate: LocalDate): Boolean

    /**
     * 사원의 특정 날짜, 특정 거래처 스케줄 조회
     */
    fun findByFullNameAndAccountAndStartDate(fullName: String, account: String, startDate: LocalDate): StoreSchedule?

    // Phase2: 키워드 검색 — 삭제된 필드(storeName, storeCode, address) 참조로 비활성화
    // @Query("""
    //     SELECT s FROM StoreSchedule s
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
    // ): List<StoreSchedule>

    /**
     * 사원의 기간 내 스케줄 전체 조회
     */
    fun findByFullNameAndStartDateBetween(
        fullName: String,
        startDateStart: LocalDate,
        startDateEnd: LocalDate
    ): List<StoreSchedule>

    /**
     * 사원의 월별 중복 제거 거래처(account sfid) 조회
     */
    @Query(
        """
        SELECT DISTINCT s.account FROM StoreSchedule s
        WHERE s.fullName = :fullName
        AND s.startDate BETWEEN :startDate AND :endDate
        """
    )
    fun findDistinctAccountsByFullNameAndStartDateBetween(
        @Param("fullName") fullName: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<String>

    /**
     * 사원의 기간 내 일정이 있는 날짜 목록 조회 (중복 제거)
     */
    @Query(
        """
        SELECT DISTINCT s.startDate FROM StoreSchedule s
        WHERE s.fullName = :fullName
        AND s.startDate BETWEEN :startDate AND :endDate
        ORDER BY s.startDate
        """
    )
    fun findDistinctStartDatesByFullNameAndDateBetween(
        @Param("fullName") fullName: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<LocalDate>
}
