package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionProduct
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long> {

    fun findByPromotionId(promotionId: Long): PromotionProduct?

    // 행사 상세의 "상세 POS품목" 섹션 — 한 Promotion 에 다수 child 허용 (V200 UNIQUE 제거).
    // is_deleted=false 만, name (PS{########}) 기준 정렬.
    fun findByPromotionIdAndIsDeletedFalseOrderByNameAsc(promotionId: Long): List<PromotionProduct>
}
