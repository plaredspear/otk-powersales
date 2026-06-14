package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionProduct

interface PromotionProductRepositoryCustom {

    /**
     * 행사 상세의 "상세 POS품목" 섹션 — 한 Promotion 에 다수 child 허용 (V200 UNIQUE 제거).
     * is_deleted=false 만, name (PS{########}) 기준 정렬.
     *
     * 응답 변환(PromotionPosProductResponse.from)이 product.name/productCode (LAZY) 를 row 마다 읽으므로
     * product 를 join fetch 하여 N+1 회피.
     */
    fun findByPromotionIdAndIsDeletedFalseOrderByNameAsc(promotionId: Long): List<PromotionProduct>
}
