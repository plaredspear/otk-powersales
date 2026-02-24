package com.otoki.internal.product.dto.response

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * FavoriteProduct Entity 구조 변경으로 from() 변환 로직이 컴파일 오류 → 주석 처리.

import com.otoki.internal.product.entity.FavoriteProduct
import java.time.format.DateTimeFormatter

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

--- */
