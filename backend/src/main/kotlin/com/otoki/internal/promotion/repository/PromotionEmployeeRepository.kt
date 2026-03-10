package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long> {

    fun findByPromotionIdOrderByScheduleDateAsc(promotionId: Long): List<PromotionEmployee>
}
