package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.domain.foundation.product.entity.QProductBarcode.Companion.productBarcode
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.activity.order.entity.QOrderRequest.Companion.orderRequest
import com.otoki.powersales.domain.activity.order.entity.QOrderRequestProduct.Companion.orderRequestProduct
import com.querydsl.core.types.dsl.Expressions
import java.time.LocalDateTime
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Expression
import com.querydsl.core.types.OrderSpecifier
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
     * 최근 주문 제품 여부 — 검색 결과 상단 정렬키.
     *
     * [employeeId] 본인이 [orderDateFrom] 이후 주문한 제품이면 true. 거래처(account) 조건은
     * 걸지 않아 거래처 미선택 상태에서도 동작한다(주문이력 탭은 거래처 AND 라 기준이 다름).
     * 주문 상태는 구분하지 않고 삭제분만 제외한다(주문이력 탭 findOrderHistory 와 동일).
     */
    private fun recentlyOrderedProduct(
        employeeId: Long,
        orderDateFrom: LocalDateTime,
    ): BooleanExpression =
        JPAExpressions.selectOne()
            .from(orderRequestProduct)
            .join(orderRequestProduct.orderRequest, orderRequest)
            .where(
                orderRequestProduct.product.id.eq(product.id),
                orderRequest.employee.id.eq(employeeId),
                orderRequest.orderDate.goe(orderDateFrom),
                orderRequest.isDeleted.isNull.or(orderRequest.isDeleted.eq(false)),
            )
            .exists()

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

    /**
     * 제품상태 필터 — 파라미터는 화면 표시명("판매중"/"단종") 으로 들어온다.
     *
     * PLACEHOLDER("-" → "판매중") 은 저장값이 없는(null) 제품을 가리키므로 eq 가 아니라 isNull 로
     * 평가해야 한다 — 저장값 "-" 는 운영 데이터에 없지만, 있더라도 같은 "판매중" 집합에 포함시킨다.
     * 미지정(null/blank) 이거나 알 수 없는 표시명이면 필터를 적용하지 않는다(null 반환).
     */
    private fun productStatusFilter(productStatus: String?): BooleanExpression? {
        if (productStatus.isNullOrBlank()) return null
        val status = ProductStatus.fromLabelOrNull(productStatus) ?: return null
        return if (status == ProductStatus.PLACEHOLDER) {
            product.productStatus.isNull.or(product.productStatus.eq(status))
        } else {
            product.productStatus.eq(status)
        }
    }

    /**
     * 발주 단위(product.unit)와 일치하는 대표 바코드 1건을 SELECT 절 상관 서브쿼리로 가져온다.
     * (레거시 selectProduct: `a.dkretail__unit__c = b.productunit__c AND b.productbarcode__c IS NOT NULL`)
     *
     * N+1 회피: @OneToMany fetch join 대신 단일 쿼리에서 함께 조회한다. 단위별 바코드가 복수일
     * 가능성에 대비해 min() 으로 단일 스칼라를 보장한다(행 증식 / count 불일치 방지).
     */
    private fun unitMatchedBarcode(): Expression<String> =
        JPAExpressions
            .select(productBarcode.barcode.min())
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.unit.eq(product.unit),
                productBarcode.barcode.isNotNull,
            )

    /**
     * 제품 + 단위 매칭 바코드를 단일 쿼리로 조회하는 공통 페이지 실행기.
     * count 는 EXISTS 기반 [where] 로 제품 단위(distinct)로 집계되어 본문 행 수와 일치한다.
     *
     * [orderBy] 기본값은 제품명/제품코드 정렬이며, 레거시 정렬이 다른 검색(예: 제품추가 팝업
     * `selectProduct` 의 `ORDER BY categorycode3, productcode`)은 호출부에서 정렬을 넘긴다.
     */
    private fun pagedSearch(
        where: BooleanExpression,
        pageable: Pageable,
        orderBy: Array<OrderSpecifier<*>> = arrayOf(product.name.asc(), product.productCode.asc()),
        recentlyOrdered: BooleanExpression? = null,
    ): Page<ProductSearchRow> {
        val matchedBarcode = unitMatchedBarcode()

        // Hibernate 6 는 SELECT 절의 벌거벗은 exists() 를 파싱하지 못하므로 CASE 로 스칼라화한다.
        val recentFlag: Expression<Int> = recentlyOrdered
            ?.let {
                Expressions.cases().`when`(it).then(1).otherwise(0)
            }
            ?: Expressions.asNumber(0).intValue()

        val rows = queryFactory
            .select(product, matchedBarcode, recentFlag)
            .from(product)
            .where(where)
            .orderBy(*orderBy)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val content = rows.map { tuple ->
            ProductSearchRow(
                product = tuple.get(product)!!,
                barcode = tuple.get(matchedBarcode),
                recentlyOrdered = tuple.get(recentFlag) == 1
            )
        }

        val countQuery = queryFactory
            .select(product.count())
            .from(product)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun searchForAdmin(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable
    ): Page<Product> {
        val builder = BooleanBuilder()

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
        productStatusFilter(productStatus)?.let { builder.and(it) }

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


    override fun searchByFilter(
        productName: String?,
        barcode: String?,
        category2: String?,
        category3: String?,
        pageable: Pageable
    ): Page<ProductSearchRow> {
        var where = orderableProductFilter()

        if (!productName.isNullOrBlank()) {
            // 레거시 `selectProduct` 의 제품명 검색은 `a.name LIKE` 단일 컬럼(제품코드 미포함).
            // 대소문자 무시를 위해 양쪽을 lower 처리한다(한글은 영향 없음).
            val pattern = "%${productName.lowercase()}%"
            where = where.and(product.name.lower().like(pattern))
        }

        if (!barcode.isNullOrBlank()) {
            // 레거시: `b.productbarcode__c LIKE '%?%' AND a.dkretail__unit__c = b.productunit__c`.
            // 발주 단위와 일치하는 바코드(목록에 표시되는 대표 바코드)에 대한 부분일치.
            val barcodePattern = "%$barcode%"
            val barcodeLikeExists = JPAExpressions.selectOne()
                .from(productBarcode)
                .where(
                    productBarcode.productId.eq(product.id),
                    productBarcode.unit.eq(product.unit),
                    productBarcode.barcode.like(barcodePattern),
                )
                .exists()
            where = where.and(barcodeLikeExists)
        }

        if (!category2.isNullOrBlank()) {
            where = where.and(product.productCategory2.eq(category2))
        }
        if (!category3.isNullOrBlank()) {
            where = where.and(product.productCategory3.eq(category3))
        }

        // 레거시 `selectProduct` 정렬: `ORDER BY categorycode3, productcode`.
        return pagedSearch(
            where,
            pageable,
            orderBy = arrayOf(product.categoryCode3.asc(), product.productCode.asc()),
        )
    }

    /**
     * 중분류/소분류 드롭다운 소스 — 레거시 `selectMiddleProduct`/`selectSmallProduct` 정합.
     *
     * 레거시 셀렉트는 발주가능(가정/업소·바코드·활성) 필터 없이 `category2 IS NOT NULL` /
     * `category3 IS NOT NULL` 만으로 distinct 조회하므로(선택 시 0건이 되는 중분류도 노출),
     * 여기서도 orderableProductFilter() 를 적용하지 않는다.
     * category3 가 없는(소분류 미지정) 제품만 가진 중분류도 중분류 목록에는 포함되도록 category3 는 nullable 로 둔다.
     */
    override fun findCategoryGroups(): List<CategoryGroupRow> {
        val results = queryFactory
            .select(product.productCategory2, product.productCategory3)
            .from(product)
            .where(product.productCategory2.isNotNull)
            .distinct()
            .orderBy(product.productCategory2.asc(), product.productCategory3.asc())
            .fetch()

        return results.mapNotNull { tuple ->
            val c2 = tuple.get(product.productCategory2) ?: return@mapNotNull null
            CategoryGroupRow(category2 = c2, category3 = tuple.get(product.productCategory3))
        }
    }

    override fun searchByText(query: String, pageable: Pageable): Page<ProductSearchRow> {
        val pattern = "%${query.lowercase()}%"

        val searchPredicate = product.name.lower().like(pattern)
            .or(product.productCode.lower().like(pattern))

        return pagedSearch(orderableProductFilter().and(searchPredicate), pageable)
    }

    override fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<ProductSearchRow> {
        // 레거시 `searchWord`: `a.name OR a.dkretail__productcode__c OR b.productbarcode__c` 3-컬럼 OR LIKE.
        // 바코드는 물류 바코드(Product.logisticsBarcode)가 아니라 소비자 바코드(ProductBarcode.barcode)를 매칭한다.
        val lowerPattern = "%${query.lowercase()}%"
        val rawPattern = "%$query%"

        val barcodeLikeExists = JPAExpressions.selectOne()
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.unit.eq(product.unit),
                productBarcode.barcode.like(rawPattern),
            )
            .exists()

        val searchPredicate = product.name.lower().like(lowerPattern)
            .or(product.productCode.lower().like(lowerPattern))
            .or(barcodeLikeExists)

        return pagedSearch(orderableProductFilter().and(searchPredicate), pageable)
    }

    override fun searchForOrder(
        query: String,
        category2: String?,
        category3: String?,
        pageable: Pageable,
        recentOrderEmployeeId: Long?,
        recentOrderFrom: LocalDateTime?
    ): Page<ProductSearchRow> {
        // 레거시 주문 `searchWord`: name OR productCode OR 소비자 바코드(ProductBarcode.barcode) OR LIKE.
        val lowerPattern = "%${query.lowercase()}%"
        val rawPattern = "%$query%"

        val barcodeLikeExists = JPAExpressions.selectOne()
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.unit.eq(product.unit),
                productBarcode.barcode.like(rawPattern),
            )
            .exists()

        var where = orderableProductFilter().and(
            product.name.lower().like(lowerPattern)
                .or(product.productCode.lower().like(lowerPattern))
                .or(barcodeLikeExists)
        )

        if (!category2.isNullOrBlank()) {
            where = where.and(product.productCategory2.eq(category2))
        }
        if (!category3.isNullOrBlank()) {
            where = where.and(product.productCategory3.eq(category3))
        }

        // 최근 주문 제품을 상단으로 올린다(요청 시에만). 그 뒤로는 레거시
        // `selectProduct` 정렬 `ORDER BY categorycode3, productcode` 를 그대로 유지한다.
        val recentlyOrdered =
            if (recentOrderEmployeeId != null && recentOrderFrom != null) {
                recentlyOrderedProduct(recentOrderEmployeeId, recentOrderFrom)
            } else {
                null
            }

        val legacyOrder = arrayOf<OrderSpecifier<*>>(
            product.categoryCode3.asc(),
            product.productCode.asc(),
        )
        // desc: 최근주문(1)이 먼저. SELECT 절과 동일하게 CASE 로 스칼라화한다
        // (Hibernate 6 는 벌거벗은 exists() 를 ORDER BY/SELECT 에서 파싱하지 못한다).
        val orderBy = if (recentlyOrdered != null) {
            arrayOf(
                Expressions.cases().`when`(recentlyOrdered).then(1).otherwise(0).desc(),
                *legacyOrder,
            )
        } else {
            legacyOrder
        }

        return pagedSearch(where, pageable, orderBy = orderBy, recentlyOrdered = recentlyOrdered)
    }

    override fun findOrderRowsByProductCodes(productCodes: Collection<String>): List<ProductSearchRow> {
        if (productCodes.isEmpty()) return emptyList()

        val matchedBarcode = unitMatchedBarcode()

        return queryFactory
            .select(product, matchedBarcode)
            .from(product)
            .where(product.productCode.`in`(productCodes))
            .fetch()
            .map { tuple ->
                ProductSearchRow(
                    product = tuple.get(product)!!,
                    barcode = tuple.get(matchedBarcode),
                )
            }
    }

    override fun findBarcodesForElectronicSales(
        productIds: List<Long>,
        category2: String?,
        category3: String?,
    ): List<String> {
        val builder = BooleanBuilder()
        builder.and(product.isDeleted.isNull.or(product.isDeleted.eq(false)))
        builder.and(productBarcode.barcode.isNotNull)
        if (productIds.isNotEmpty()) {
            builder.and(product.id.`in`(productIds))
        }
        if (!category2.isNullOrBlank()) {
            builder.and(product.productCategory2.eq(category2))
        }
        if (!category3.isNullOrBlank()) {
            builder.and(product.productCategory3.eq(category3))
        }

        return queryFactory
            .select(productBarcode.barcode)
            .from(productBarcode)
            .join(product).on(product.id.eq(productBarcode.productId))
            .where(builder)
            .distinct()
            .fetch()
            .filterNotNull()
            .filter { it.isNotBlank() }
    }

    override fun searchForElectronicSales(keyword: String, limit: Long): List<ElectronicSalesProductLookupRow> {
        val lowerPattern = "%${keyword.lowercase()}%"
        val rawPattern = "%$keyword%"

        val representativeBarcode = productBarcode.barcode.min()

        return queryFactory
            .select(product.id, product.name, product.productCode, representativeBarcode)
            .from(product)
            .join(productBarcode).on(
                productBarcode.productId.eq(product.id),
                productBarcode.barcode.isNotNull,
            )
            .where(
                product.isDeleted.isNull.or(product.isDeleted.eq(false)),
                product.name.lower().like(lowerPattern)
                    .or(product.productCode.lower().like(lowerPattern))
                    .or(productBarcode.barcode.like(rawPattern)),
            )
            .groupBy(product.id, product.name, product.productCode)
            .orderBy(product.name.asc(), product.productCode.asc())
            .limit(limit)
            .fetch()
            .map { tuple ->
                ElectronicSalesProductLookupRow(
                    productId = tuple.get(product.id) ?: 0L,
                    name = tuple.get(product.name),
                    productCode = tuple.get(product.productCode),
                    barcode = tuple.get(representativeBarcode),
                )
            }
    }

    override fun searchForElectronicSalesAdvanced(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable,
    ): Page<ElectronicSalesProductAdvancedRow> {
        val builder = BooleanBuilder()

        builder.and(product.isDeleted.isNull.or(product.isDeleted.eq(false)))

        // 소비자 바코드 보유 제품 한정 — 드롭다운 빠른 검색(searchForElectronicSales) 과 동일 집합.
        // JOIN 대신 EXISTS 를 쓰는 이유: 제품당 바코드가 여러 건이라 JOIN 시 중복 행이 생겨
        // 페이징 total 이 부풀고 offset 이 어긋난다.
        val barcodeExists = JPAExpressions.selectOne()
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.barcode.isNotNull,
            )
            .exists()
        builder.and(barcodeExists)

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            val rawPattern = "%$keyword%"
            // 소비자 바코드 부분일치는 EXISTS 서브쿼리로 — 위와 같은 이유(중복 행 방지).
            val barcodeLikeExists = JPAExpressions.selectOne()
                .from(productBarcode)
                .where(
                    productBarcode.productId.eq(product.id),
                    productBarcode.barcode.like(rawPattern),
                )
                .exists()
            builder.and(
                product.name.lower().like(lowerPattern)
                    .or(product.productCode.lower().like(lowerPattern))
                    .or(barcodeLikeExists)
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
        productStatusFilter(productStatus)?.let { builder.and(it) }

        // 대표 바코드는 SELECT 절 상관 서브쿼리로 함께 가져온다 — JOIN 이면 바코드 다건 제품이
        // 행 증식을 일으켜 페이징이 어긋난다. min() 으로 단일 스칼라를 보장한다.
        val representativeBarcode = JPAExpressions
            .select(productBarcode.barcode.min())
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.barcode.isNotNull,
            )

        val content = queryFactory
            .select(product, representativeBarcode)
            .from(product)
            .where(builder)
            .orderBy(product.name.asc(), product.productCode.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { tuple ->
                ElectronicSalesProductAdvancedRow(
                    product = tuple.get(product)!!,
                    barcode = tuple.get(representativeBarcode),
                )
            }

        val countQuery = queryFactory
            .select(product.count())
            .from(product)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findByBarcode(barcode: String, pageable: Pageable): Page<ProductSearchRow> {
        // 레거시 selectProduct: `b.productbarcode__c LIKE '%?%' AND a.dkretail__unit__c = b.productunit__c`.
        // 스캐너가 읽는 소비자 바코드는 ProductBarcode.barcode 에 저장되므로(물류 바코드 아님)
        // 발주 단위와 일치하는 바코드에 대한 부분일치 + orderable 필터로 조회한다.
        val barcodePattern = "%$barcode%"
        val barcodeLikeExists = JPAExpressions.selectOne()
            .from(productBarcode)
            .where(
                productBarcode.productId.eq(product.id),
                productBarcode.unit.eq(product.unit),
                productBarcode.barcode.like(barcodePattern),
            )
            .exists()

        return pagedSearch(orderableProductFilter().and(barcodeLikeExists), pageable)
    }
}
