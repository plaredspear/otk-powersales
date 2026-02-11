package com.otoki.internal.repository

import com.otoki.internal.entity.InspectionTheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 현장 점검 테마 Repository
 */
@Repository
interface InspectionThemeRepository : JpaRepository<InspectionTheme, Long> {

    /**
     * 특정 날짜 기준 활성 테마 조회
     * startDate <= targetDate <= endDate 조건
     * 테마명 기준 가나다순 정렬
     */
    @Query(
        "SELECT t FROM InspectionTheme t " +
        "WHERE t.isActive = true " +
        "AND t.startDate <= :targetDate " +
        "AND t.endDate >= :targetDate " +
        "ORDER BY t.name ASC"
    )
    fun findActiveThemesByDate(
        @Param("targetDate") targetDate: LocalDate
    ): List<InspectionTheme>
}
