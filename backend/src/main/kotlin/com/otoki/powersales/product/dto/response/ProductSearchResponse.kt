package com.otoki.powersales.product.dto.response

import com.otoki.powersales.product.entity.Product

/**
 * 제품 검색 응답 DTO
 */
data class ProductDto(
    val productCode: String?,
    val productName: String?,
    /** 발주 단위(product.unit) 매칭 대표 바코드 (레거시 productbarcode__c). 목록 화면 "바코드" 표시값. */
    val barcode: String?,
    val logisticsBarcode: String?,
    val storageCondition: String?,
    val shelfLife: String?,
    /** 유통기한 단위 (예: "개월"). 화면에서 shelfLife 와 결합해 "9개월"로 표시. */
    val shelfLifeUnit: String?,
    val category1: String?,
    val category2: String?
) {
    companion object {
        fun from(product: Product, barcode: String? = null): ProductDto {
            return ProductDto(
                productCode = product.productCode,
                productName = product.name,
                barcode = barcode,
                logisticsBarcode = product.logisticsBarcode,
                storageCondition = product.storageCondition?.displayName,
                shelfLife = product.shelfLife,
                shelfLifeUnit = product.shelfLifeUnit,
                category1 = product.productCategory1,
                category2 = product.productCategory2
            )
        }
    }
}
