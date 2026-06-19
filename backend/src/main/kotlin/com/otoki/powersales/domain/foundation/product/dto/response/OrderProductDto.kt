package com.otoki.powersales.domain.foundation.product.dto.response

import com.otoki.powersales.domain.foundation.product.entity.Product

/**
 * 주문 작성용 제품 검색 응답 DTO.
 *
 * 모바일 `ProductForOrderModel` 과 1:1 정합한다. 주문 라인 생성에 필요한 단가/박스입수와
 * 추가 차단 룰 판정값(전용상품/시식·증정용)을 함께 내려준다.
 *
 * 차단 룰 매핑(레거시 order/write.jsp poplayer.js 정합):
 *  - `productType == "2"` → "EXCLUSIVE" (전용상품 추가 차단)
 *  - `tasteGift == "x"/"X"` → "TASTING_GIFT" (시식·증정용 추가 차단)
 */
data class OrderProductDto(
    val productCode: String,
    val productName: String,
    val barcode: String,
    /** 보관 조건 (실온/냉장 등). */
    val storageType: String,
    /** 유통기한 (shelfLife + shelfLifeUnit 결합, 예: "9개월"). */
    val shelfLife: String,
    val unitPrice: Int,
    val boxSize: Int,
    val isFavorite: Boolean,
    val categoryMid: String?,
    val categorySub: String?,
    /** "EXCLUSIVE" 면 전용상품(추가 차단). 그 외 null. */
    val productType: String?,
    /** "TASTING_GIFT" 면 시식·증정용(추가 차단). 그 외 null. */
    val tasteGiftType: String?
) {
    companion object {
        /** 레거시 전용상품 판정값 (DKRetail__ProductType__c). */
        private const val EXCLUSIVE_PRODUCT_TYPE = "2"

        fun from(product: Product, barcode: String? = null): OrderProductDto {
            val shelf = listOfNotNull(product.shelfLife, product.shelfLifeUnit)
                .joinToString("")

            return OrderProductDto(
                productCode = product.productCode ?: "",
                productName = product.name ?: "",
                barcode = barcode ?: product.logisticsBarcode ?: "",
                storageType = product.storageCondition?.displayName ?: "",
                shelfLife = shelf,
                // 레거시 정합: 낱개단가 = 표준단가 + 주세(supertax). (orderMapper selectPrd / SF Flow 동일)
                unitPrice = ((product.standardUnitPrice ?: java.math.BigDecimal.ZERO) +
                    (product.superTax ?: java.math.BigDecimal.ZERO)).toInt(),
                boxSize = product.boxReceivingQuantity?.toInt() ?: 0,
                isFavorite = false,
                categoryMid = product.productCategory2,
                categorySub = product.productCategory3,
                productType = if (product.productType?.displayName == EXCLUSIVE_PRODUCT_TYPE) "EXCLUSIVE" else null,
                tasteGiftType = if (product.tasteGift?.equals("x", ignoreCase = true) == true) "TASTING_GIFT" else null
            )
        }
    }
}
