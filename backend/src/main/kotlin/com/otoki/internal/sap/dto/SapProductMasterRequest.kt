package com.otoki.internal.sap.dto

data class SapProductMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val productCode: String? = null,
        val productName: String? = null,
        val productBarcode: String? = null,
        val logisticsBarcode: String? = null,
        val categoryCode1: String? = null,
        val category1: String? = null,
        val categoryCode2: String? = null,
        val category2: String? = null,
        val categoryCode3: String? = null,
        val category3: String? = null,
        val productStatus: String? = null,
        val standardPrice: String? = null,
        val unit: String? = null,
        val boxReceivingQuantity: String? = null,
        val shelfLife: String? = null,
        val shelfLifeUnit: String? = null,
        val launchDate: String? = null,
        val storeCondition: String? = null,
        val productType: String? = null,
        val superTax: String? = null,
        val tasteGift: String? = null,
        val pallet: String? = null
    )
}
