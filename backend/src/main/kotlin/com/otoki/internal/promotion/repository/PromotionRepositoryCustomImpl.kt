package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.QPromotion.promotion
import com.otoki.internal.sap.entity.QProduct.product
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class PromotionRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PromotionRepositoryCustom {

    override fun searchForAdmin(
        keyword: String?,
        promotionType: String?,
        category: String?,
        startDate: String?,
        endDate: String?,
        branchCodes: List<String>?,
        pageable: Pageable
    ): Page<Promotion> {
        val builder = BooleanBuilder()

        builder.and(promotion.isDeleted.eq(false))

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                promotion.promotionName.lower().like(lowerPattern)
                    .or(promotion.promotionNumber.lower().like(lowerPattern))
            )
        }

        if (!promotionType.isNullOrBlank()) {
            builder.and(promotion.promotionType.eq(promotionType))
        }

        if (!startDate.isNullOrBlank()) {
            val date = LocalDate.parse(startDate)
            builder.and(promotion.endDate.goe(date))
        }

        if (!endDate.isNullOrBlank()) {
            val date = LocalDate.parse(endDate)
            builder.and(promotion.startDate.loe(date))
        }

        if (branchCodes != null) {
            builder.and(promotion.costCenterCode.`in`(branchCodes))
        }

        // category 필터: Product 조인
        val needsProductJoin = !category.isNullOrBlank()

        if (needsProductJoin) {
            builder.and(product.category1.eq(category))
        }

        val query = queryFactory
            .selectFrom(promotion)

        if (needsProductJoin) {
            query.leftJoin(product).on(promotion.primaryProductId.eq(product.id))
        }

        val content = query
            .where(builder)
            .orderBy(promotion.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(promotion.count())
            .from(promotion)

        if (needsProductJoin) {
            countQuery.leftJoin(product).on(promotion.primaryProductId.eq(product.id))
        }

        countQuery.where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
