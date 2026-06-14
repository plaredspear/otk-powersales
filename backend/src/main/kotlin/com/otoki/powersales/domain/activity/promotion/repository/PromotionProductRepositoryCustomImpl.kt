package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionProduct
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.domain.activity.promotion.entity.QPromotionProduct.Companion.promotionProduct
import com.querydsl.jpa.impl.JPAQueryFactory

class PromotionProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PromotionProductRepositoryCustom {

    override fun findByPromotionIdAndIsDeletedFalseOrderByNameAsc(promotionId: Long): List<PromotionProduct> {
        return queryFactory
            .selectFrom(promotionProduct)
            .leftJoin(promotionProduct.product, product).fetchJoin()
            .where(
                promotionProduct.promotionId.eq(promotionId),
                promotionProduct.isDeleted.eq(false),
            )
            .orderBy(promotionProduct.name.asc())
            .fetch()
    }
}
