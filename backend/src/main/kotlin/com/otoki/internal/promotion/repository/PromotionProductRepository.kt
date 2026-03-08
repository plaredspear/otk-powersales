package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.PromotionProduct
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long> {

    fun findByPromotionIdAndIsMainProduct(promotionId: Long, isMainProduct: Boolean): PromotionProduct?

    fun deleteByPromotionIdAndIsMainProduct(promotionId: Long, isMainProduct: Boolean)
}
