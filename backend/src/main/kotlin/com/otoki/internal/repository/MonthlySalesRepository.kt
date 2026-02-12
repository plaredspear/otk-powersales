package com.otoki.internal.repository

import com.otoki.internal.entity.MonthlySales
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 월매출 Repository
 */
interface MonthlySalesRepository : JpaRepository<MonthlySales, Long> {

    /**
     * 특정 고객, 특정 연월의 월매출 데이터 조회
     */
    fun findByCustomerIdAndYearMonth(customerId: String, yearMonth: String): List<MonthlySales>

    /**
     * 특정 고객, 연도 범위의 월매출 데이터 조회
     */
    @Query(
        """
        SELECT m FROM MonthlySales m
        WHERE m.customerId = :customerId
          AND m.yearMonth >= :startYearMonth
          AND m.yearMonth <= :endYearMonth
        """
    )
    fun findByCustomerIdAndYearMonthRange(
        @Param("customerId") customerId: String,
        @Param("startYearMonth") startYearMonth: String,
        @Param("endYearMonth") endYearMonth: String
    ): List<MonthlySales>
}
