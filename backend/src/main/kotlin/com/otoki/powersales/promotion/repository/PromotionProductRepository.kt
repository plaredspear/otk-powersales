package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionProduct
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long> {

    fun findByPromotionId(promotionId: Long): PromotionProduct?
}
