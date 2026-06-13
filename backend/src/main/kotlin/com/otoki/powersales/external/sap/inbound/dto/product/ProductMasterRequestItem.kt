package com.otoki.powersales.external.sap.inbound.dto.product

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 제품 마스터 행 DTO. (Spec #559)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (`@JsonProperty` 명시 바인딩).
 * 신규 [com.otoki.powersales.domain.foundation.product.entity.Product] 에 컬럼이 없는 필드는 수신은 하되 무시된다 (D1).
 */
data class ProductMasterRequestItem(
    @JsonProperty("ProductCode") val productCode: String? = null,
    @JsonProperty("ProductName") val productName: String? = null,
    @JsonProperty("ProductBarcode") val productBarcode: String? = null,
    @JsonProperty("LogisticsBarCode") val logisticsBarCode: String? = null,
    @JsonProperty("CategoryCode1") val categoryCode1: String? = null,
    @JsonProperty("Category1") val category1: String? = null,
    @JsonProperty("CategoryCode2") val categoryCode2: String? = null,
    @JsonProperty("Category2") val category2: String? = null,
    @JsonProperty("CategoryCode3") val categoryCode3: String? = null,
    @JsonProperty("Category3") val category3: String? = null,
    @JsonProperty("ProductStatus") val productStatus: String? = null,
    @JsonProperty("StandardPrice") val standardPrice: String? = null,
    @JsonProperty("Unit") val unit: String? = null,
    @JsonProperty("BoxReceivingQuantity") val boxReceivingQuantity: String? = null,
    @JsonProperty("ShelfLife") val shelfLife: String? = null,
    @JsonProperty("ShelfLifeUnit") val shelfLifeUnit: String? = null,
    @JsonProperty("LaunchDate") val launchDate: String? = null,
    @JsonProperty("StoreCondition") val storeCondition: String? = null,
    @JsonProperty("ProductType") val productType: String? = null,
    @JsonProperty("SuperTax") val superTax: String? = null,
    @JsonProperty("TasteGift") val tasteGift: String? = null,
    @JsonProperty("Pallet") val pallet: String? = null
)
