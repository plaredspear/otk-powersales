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
}
