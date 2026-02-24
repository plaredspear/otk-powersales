package com.otoki.internal.product.dto.response

import com.otoki.internal.product.entity.Product

/**
 * 제품 검색 응답 DTO
 */
data class ProductDto(
    val productCode: String?,
    val productName: String?,
    val logisticsBarcode: String?,
    val storageCondition: String?,
    val shelfLife: String?,
    val category1: String?,
    val category2: String?
) {
    companion object {
        fun from(product: Product): ProductDto {
            return ProductDto(
                productCode = product.productCode,
                productName = product.name,
                logisticsBarcode = product.logisticsBarcode,
                storageCondition = product.storageCondition,
                shelfLife = product.shelfLife,
                category1 = product.category1,
                category2 = product.category2
            )
        }
    }
}
