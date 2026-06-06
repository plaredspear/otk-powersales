package com.otoki.powersales.product.repository

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.ProductStatus
import com.otoki.powersales.product.entity.QProduct.Companion.product
import com.otoki.powersales.product.entity.QProductBarcode.Companion.productBarcode
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class ProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    companion object {
        /** 레거시 제품검색 소분류(category3) 고정 필터 값 (label.properties: 가정/업소). */
        private val ORDERABLE_CATEGORY3 = listOf("가정", "업소")
    }

    /**
     * 모바일 제품검색(영업사원용) 고정 필터 — 레거시 productMapper.xml `selectProduct` 의
     * 고정 WHERE 조건을 이식한다.
     *  1) 발주 단위(product.unit)와 일치하는 바코드가 등록된 제품만
     *     (레거시: `b.productbarcode__c IS NOT NULL AND a.dkretail__unit__c = b.productunit__c`)
     *  2) 소분류(category3) = '가정' 또는 '업소'
     *  3) productStatus IS NULL (활성 제품 — 단종/숨김 등 상태값이 찍힌 제품 제외)
     */
    private fun orderableProductFilter(): BooleanExpression {
        val unitMatchedBarcodeExists = JPAExpressions.selectOne()
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.unit.eq(product.unit),
                productBarcode.barcode.isNotNull,
            )
            .exists()

        return unitMatchedBarcodeExists
            .and(product.productCategory3.`in`(ORDERABLE_CATEGORY3))
            .and(product.productStatus.isNull)
    }

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
            .orderBy(product.name.asc(), product.productCode.asc())
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

        val searchPredicate = product.name.lower().like(pattern)
            .or(product.productCode.lower().like(pattern))
        val where = orderableProductFilter().and(searchPredicate)

        val content = queryFactory
            .selectFrom(product)
            .where(where)
            .orderBy(product.name.asc(), product.productCode.asc())
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

        val searchPredicate = product.name.lower().like(lowerPattern)
            .or(product.productCode.lower().like(lowerPattern))
            .or(product.logisticsBarcode.like(rawPattern))
        val where = orderableProductFilter().and(searchPredicate)

        val content = queryFactory
            .selectFrom(product)
            .where(where)
            .orderBy(product.name.asc(), product.productCode.asc())
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
