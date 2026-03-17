package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long> {

    fun findByPromotionIdOrderByScheduleDateAsc(promotionId: Long): List<PromotionEmployee>

    fun findByPromotionId(promotionId: Long): List<PromotionEmployee>

    fun existsByPromotionIdAndPromoCloseByTmTrue(promotionId: Long): Boolean

    fun deleteByPromotionId(promotionId: Long)

    @Query("SELECT MIN(pe.scheduleDate) FROM PromotionEmployee pe WHERE pe.promotionId = :promotionId")
    fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate?

    @Query("SELECT MAX(pe.scheduleDate) FROM PromotionEmployee pe WHERE pe.promotionId = :promotionId")
    fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate?

    @Query("SELECT COALESCE(SUM(pe.targetAmount), 0) FROM PromotionEmployee pe WHERE pe.promotionId = :promotionId")
    fun sumTargetAmountByPromotionId(promotionId: Long): Long

    @Query("SELECT COALESCE(SUM(pe.actualAmount), 0) FROM PromotionEmployee pe WHERE pe.promotionId = :promotionId")
    fun sumActualAmountByPromotionId(promotionId: Long): Long

    /**
     * 행사별 + 사번별 최초 투입일 조회 (모바일 행사 목록용)
     */
    @Query("SELECT MIN(pe.scheduleDate) FROM PromotionEmployee pe WHERE pe.promotionId = :promotionId AND pe.employeeId = :employeeId")
    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: String): LocalDate?

    /**
     * 사번으로 배정된 행사의 PromotionEmployee 존재 여부 확인 (모바일 상세 접근 검증용)
     */
    fun existsByPromotionIdAndEmployeeId(promotionId: Long, employeeId: String): Boolean
}
