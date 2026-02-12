package com.otoki.internal.repository

import com.otoki.internal.entity.StoreSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface StoreScheduleRepository : JpaRepository<StoreSchedule, Long> {

    /**
     * 사용자의 특정 날짜 거래처 스케줄 조회
     */
    fun findByUserIdAndScheduleDate(userId: Long, scheduleDate: LocalDate): List<StoreSchedule>

    /**
     * 사용자의 특정 날짜, 특정 거래처 스케줄 존재 여부
     */
    fun existsByUserIdAndStoreIdAndScheduleDate(userId: Long, storeId: Long, scheduleDate: LocalDate): Boolean

    /**
     * 사용자의 특정 날짜, 특정 거래처 스케줄 조회
     */
    fun findByUserIdAndStoreIdAndScheduleDate(userId: Long, storeId: Long, scheduleDate: LocalDate): StoreSchedule?

    /**
     * 키워드로 거래처 검색 (거래처명, 주소, 거래처코드)
     */
    @Query(
        """
        SELECT s FROM StoreSchedule s
        WHERE s.userId = :userId
        AND s.scheduleDate = :scheduleDate
        AND (
            LOWER(s.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.storeCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """
    )
    fun findByUserIdAndScheduleDateAndKeyword(
        @Param("userId") userId: Long,
        @Param("scheduleDate") scheduleDate: LocalDate,
        @Param("keyword") keyword: String
    ): List<StoreSchedule>

    /**
     * 사용자의 기간 내 스케줄 전체 조회
     */
    fun findByUserIdAndScheduleDateBetween(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StoreSchedule>

    /**
     * 사용자의 월별 중복 제거 거래처 ID 조회
     * 내 거래처 기능에서 한 달 일정의 거래처를 중복 없이 조회
     */
    @Query(
        """
        SELECT DISTINCT s.storeId FROM StoreSchedule s
        WHERE s.userId = :userId
        AND s.scheduleDate BETWEEN :startDate AND :endDate
        """
    )
    fun findDistinctStoreIdsByUserIdAndScheduleDateBetween(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Long>

    /**
     * 사용자의 기간 내 일정이 있는 날짜 목록 조회 (중복 제거)
     * 월간 캘린더에서 근무일 여부 표시용
     */
    @Query(
        """
        SELECT DISTINCT s.scheduleDate FROM StoreSchedule s
        WHERE s.userId = :userId
        AND s.scheduleDate BETWEEN :startDate AND :endDate
        ORDER BY s.scheduleDate
        """
    )
    fun findDistinctScheduleDatesByUserIdAndDateBetween(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<LocalDate>
}
