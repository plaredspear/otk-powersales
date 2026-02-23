package com.otoki.internal.dto.response

import com.otoki.internal.entity.FavoriteProduct
import java.time.format.DateTimeFormatter

/**
 * 즐겨찾기 제품 응답 DTO
 */
data class FavoriteProductResponse(
    val productCode: String?,
    val productName: String?,
    val logisticsBarcode: String?,
    val storageCondition: String?,
    val category1: String?,
    val category2: String?,
    val addedAt: String
) {
    companion object {
        fun from(favorite: FavoriteProduct): FavoriteProductResponse {
            val product = favorite.product
            return FavoriteProductResponse(
                productCode = product.productCode,
                productName = product.name,
                logisticsBarcode = product.logisticsBarcode,
                storageCondition = product.storageCondition,
                category1 = product.category1,
                category2 = product.category2,
                addedAt = favorite.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }
}
