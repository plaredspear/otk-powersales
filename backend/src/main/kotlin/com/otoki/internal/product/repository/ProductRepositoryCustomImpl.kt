package com.otoki.internal.product.repository

import com.otoki.internal.product.entity.Product
import com.otoki.internal.product.entity.QProduct.product
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class ProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    override fun searchByText(query: String, pageable: Pageable): Page<Product> {
        val pattern = "%${query.lowercase()}%"

        val where = product.name.lower().like(pattern)
            .or(product.productCode.lower().like(pattern))

        val content = queryFactory
            .selectFrom(product)
            .where(where)
            .orderBy(product.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(product.count())
            .from(product)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<Product> {
        val lowerPattern = "%${query.lowercase()}%"
        val rawPattern = "%$query%"

        val where = product.name.lower().like(lowerPattern)
            .or(product.productCode.lower().like(lowerPattern))
            .or(product.logisticsBarcode.like(rawPattern))

        val content = queryFactory
            .selectFrom(product)
            .where(where)
            .orderBy(product.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(product.count())
            .from(product)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
