package com.otoki.powersales.product.repository

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.ProductStatus
import com.otoki.powersales.product.entity.QProduct.Companion.product
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
            builder.and(product.productCategory1.eq(category1))
        }
        if (!category2.isNullOrBlank()) {
            builder.and(product.productCategory2.eq(category2))
        }
        if (!category3.isNullOrBlank()) {
            builder.and(product.productCategory3.eq(category3))
        }
        if (!productStatus.isNullOrBlank()) {
            ProductStatus.fromDisplayNameOrNull(productStatus)?.let {
                builder.and(product.productStatus.eq(it))
            }
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
            .select(product.productCategory1, product.productCategory2, product.productCategory3)
            .from(product)
            .where(
                product.isDeleted.isNull.or(product.isDeleted.eq(false)),
                product.productCategory1.isNotNull,
                product.productCategory2.isNotNull,
                product.productCategory3.isNotNull
            )
            .distinct()
            .orderBy(product.productCategory1.asc(), product.productCategory2.asc(), product.productCategory3.asc())
            .fetch()

        return results.mapNotNull { tuple ->
            val c1 = tuple.get(product.productCategory1) ?: return@mapNotNull null
            val c2 = tuple.get(product.productCategory2) ?: return@mapNotNull null
            val c3 = tuple.get(product.productCategory3) ?: return@mapNotNull null
            CategoryRow(category1 = c1, category2 = c2, category3 = c3)
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
