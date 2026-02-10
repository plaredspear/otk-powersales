package com.otoki.internal.dto.response

import com.otoki.internal.entity.FavoriteProduct
import java.time.format.DateTimeFormatter

/**
 * 즐겨찾기 제품 응답 DTO
 */
data class FavoriteProductResponse(
    val productCode: String,
    val productName: String,
    val barcode: String,
    val storageType: String,
    val categoryMid: String?,
    val categorySub: String?,
    val addedAt: String
) {
    companion object {
        fun from(favorite: FavoriteProduct): FavoriteProductResponse {
            val product = favorite.product
            return FavoriteProductResponse(
                productCode = product.productCode,
                productName = product.productName,
                barcode = product.barcode,
                storageType = product.storageType,
                categoryMid = product.categoryMid,
                categorySub = product.categorySub,
                addedAt = favorite.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }
}
