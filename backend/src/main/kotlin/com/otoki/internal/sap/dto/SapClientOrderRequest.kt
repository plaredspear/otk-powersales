package com.otoki.internal.sap.dto

data class SapClientOrderRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val sapOrderNumber: String? = null,
        val sapAccountCode: String? = null,
        val sapAccountName: String? = null,
        val deliveryRequestDate: String? = null,
        val orderDate: String? = null,
        val employeeCode: String? = null,
        val employeeName: String? = null,
        val orderSalesAmount: String? = null,
        val orderChannel: String? = null,
        val orderChannelNm: String? = null,
        val orderType: String? = null,
        val orderTypeNm: String? = null,
        val itemDetailList: List<ItemDetail> = emptyList()
    )

    data class ItemDetail(
        val sapOrderNumber: String? = null,
        val lineNumber: String? = null,
        val productCode: String? = null,
        val productName: String? = null,
        val orderQuantity: String? = null,
        val unit: String? = null,
        val confirmQuantityBox: String? = null,
        val confirmQuantity: String? = null,
        val confirmUnit: String? = null,
        val defaultReason: String? = null,
        val lineItemStatus: String? = null,
        val shippingDriverName: String? = null,
        val shippingVehicle: String? = null,
        val shippingDriverPhone: String? = null,
        val shippingScheduleTime: String? = null,
        val shippingCompleteTime: String? = null,
        val shippingQuantityBox: String? = null,
        val shippingQuantity: String? = null,
        val orderSalesLineAmount: String? = null,
        val shippingAmount: String? = null,
        val plant: String? = null,
        val plantNm: String? = null,
        val releaseQuantity: String? = null,
        val releaseAmount: String? = null
    )
}
