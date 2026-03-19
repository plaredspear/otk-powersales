package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long>, PromotionEmployeeRepositoryCustom {

    fun findByPromotionIdOrderByScheduleDateAsc(promotionId: Long): List<PromotionEmployee>

    fun findByPromotionId(promotionId: Long): List<PromotionEmployee>

    fun existsByPromotionIdAndPromoCloseByTmTrue(promotionId: Long): Boolean

    fun deleteByPromotionId(promotionId: Long)

    fun existsByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): Boolean
}
