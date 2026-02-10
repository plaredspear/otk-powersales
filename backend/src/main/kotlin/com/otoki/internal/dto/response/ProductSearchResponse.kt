package com.otoki.internal.dto.response

import com.otoki.internal.entity.Product

/**
 * 제품 검색 응답 DTO
 */
data class ProductDto(
    val productId: String,
    val productName: String,
    val productCode: String,
    val barcode: String,
    val storageType: String,
    val shelfLife: String?,
    val categoryMid: String?,
    val categorySub: String?
) {
    companion object {
        fun from(product: Product): ProductDto {
            return ProductDto(
                productId = product.productId,
                productName = product.productName,
                productCode = product.productCode,
                barcode = product.barcode,
                storageType = product.storageType,
                shelfLife = product.shelfLife,
                categoryMid = product.categoryMid,
                categorySub = product.categorySub
            )
        }
    }
}
