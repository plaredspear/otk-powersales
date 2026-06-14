package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.PromotionProduct
import java.math.BigDecimal

/**
 * 행사 상세 "상세 POS품목" 섹션 row (DKRetail__PromotionProduct__c).
 *
 * SF Promotion 상세 페이지의 Related List 컬럼 (Name / 제품 / 금액) 동등.
 */
data class PromotionPosProductResponse(
    val id: Long,
    val name: String?,
    val productId: Long?,
    val productName: String?,
    val productCode: String?,
    val price: BigDecimal?,
) {
    companion object {
        fun from(entity: PromotionProduct): PromotionPosProductResponse =
            PromotionPosProductResponse(
                id = entity.id,
                name = entity.name,
                productId = entity.productId,
                productName = entity.product?.name,
                productCode = entity.product?.productCode,
                price = entity.price,
            )
    }
}
