package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Product

data class ProductListResponse(
    val content: List<ProductListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class ProductListItem(
    val productCode: String?,
    val name: String?,
    val category1: String?,
    val category2: String?,
    val category3: String?,
    val standardPrice: Double?,
    val unit: String?,
    val storageCondition: String?,
    val productStatus: String?,
    val launchDate: String?
) {
    companion object {
        fun from(product: Product): ProductListItem = ProductListItem(
            productCode = product.productCode,
            name = product.name,
            category1 = product.category1,
            category2 = product.category2,
            category3 = product.category3,
            standardPrice = product.standardPrice,
            unit = product.unit,
            storageCondition = product.storageCondition,
            productStatus = product.productStatus,
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
