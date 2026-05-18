package com.otoki.powersales.product.dto.response

import com.otoki.powersales.product.entity.Product

data class ProductListResponse(
    val content: List<ProductListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class ProductListItem(
    val id: Long,
    val productCode: String?,
    val name: String?,
    val category1: String?,
    val category2: String?,
    val category3: String?,
    val standardUnitPrice: java.math.BigDecimal?,
    val unit: String?,
    val storageCondition: String?,
    val productStatus: String?,
    val launchDate: String?
) {
    companion object {
        fun from(product: Product): ProductListItem = ProductListItem(
            id = product.id,
            productCode = product.productCode,
            name = product.name,
            category1 = product.productCategory1,
            category2 = product.productCategory2,
            category3 = product.productCategory3,
            standardUnitPrice = product.standardUnitPrice,
            unit = product.unit,
            storageCondition = product.storageCondition?.displayName,
            productStatus = product.productStatus?.displayName,
            launchDate = product.launchDate?.toString()
        )
    }
}

data class CategoryTree(
    val category1: String,
    val children: List<Category2Node>
)

data class Category2Node(
    val category2: String,
    val children: List<String>
)
