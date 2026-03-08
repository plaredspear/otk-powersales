package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.QProduct.product
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class ProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    override fun searchForAdmin(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable
    ): Page<Product> {
        val builder = com.querydsl.core.BooleanBuilder()

        builder.and(product.isDeleted.isNull.or(product.isDeleted.eq(false)))

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            val rawPattern = "%$keyword%"
            builder.and(
                product.name.lower().like(lowerPattern)
                    .or(product.productCode.lower().like(lowerPattern))
                    .or(product.logisticsBarcode.like(rawPattern))
            )
        }

        if (!category1.isNullOrBlank()) {
            builder.and(product.category1.eq(category1))
        }
        if (!category2.isNullOrBlank()) {
            builder.and(product.category2.eq(category2))
        }
        if (!category3.isNullOrBlank()) {
            builder.and(product.category3.eq(category3))
        }
        if (!productStatus.isNullOrBlank()) {
            builder.and(product.productStatus.eq(productStatus))
        }

        val content = queryFactory
            .selectFrom(product)
            .where(builder)
            .orderBy(product.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(product.count())
            .from(product)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findDistinctCategories(): List<CategoryRow> {
        val results = queryFactory
            .select(product.category1, product.category2, product.category3)
            .from(product)
            .where(
                product.isDeleted.isNull.or(product.isDeleted.eq(false)),
                product.category1.isNotNull,
                product.category2.isNotNull,
                product.category3.isNotNull
            )
            .distinct()
            .orderBy(product.category1.asc(), product.category2.asc(), product.category3.asc())
            .fetch()

        return results.map { tuple ->
            CategoryRow(
                category1 = tuple.get(product.category1)!!,
                category2 = tuple.get(product.category2)!!,
                category3 = tuple.get(product.category3)!!
            )
        }
    }


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
