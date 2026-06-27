package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionEmployeeRepository : JpaRepository<PromotionEmployee, Long>, PromotionEmployeeRepositoryCustom {

    fun deleteByPromotionId(promotionId: Long)
}
