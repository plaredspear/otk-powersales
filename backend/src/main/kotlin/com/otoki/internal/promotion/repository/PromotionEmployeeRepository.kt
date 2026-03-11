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
}
