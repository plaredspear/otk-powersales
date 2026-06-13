package com.otoki.powersales.external.sap.inbound.dto.order

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP ERP 주문 라인 행 DTO. (Spec #561)
 *
 * UPSERT 키 externalKey 는 `SAPOrderNumber(선두 0 1자 제거) + LineNumber` 로 도출한다.
 * `ShippingVehicle` 은 키 구성에서 제외 — 차량 후속 갱신 호출에서 동일 라인을 UPDATE 해야 한다.
 */
data class ErpOrderItemDetail(
    @JsonProperty("SAPOrderNumber") val sapOrderNumber: String? = null,
    @JsonProperty("LineNumber") val lineNumber: String? = null,
    @JsonProperty("ProductCode") val productCode: String? = null,
    @JsonProperty("ProductName") val productName: String? = null,
    @JsonProperty("OrderQuantity") val orderQuantity: String? = null,
    @JsonProperty("Unit") val unit: String? = null,
    @JsonProperty("ConfirmQuantity_Box") val confirmQuantityBox: String? = null,
    @JsonProperty("ConfirmQuantity") val confirmQuantity: String? = null,
    @JsonProperty("Confirm_Unit") val confirmUnit: String? = null,
    @JsonProperty("DefaultReason") val defaultReason: String? = null,
    @JsonProperty("LineItemStatus") val lineItemStatus: String? = null,
    @JsonProperty("ShippingDriverName") val shippingDriverName: String? = null,
    @JsonProperty("ShippingVehicle") val shippingVehicle: String? = null,
    @JsonProperty("ShippingDriverPhone") val shippingDriverPhone: String? = null,
    @JsonProperty("ShippingScheduleTime") val shippingScheduleTime: String? = null,
    @JsonProperty("ShippingCompleteTime") val shippingCompleteTime: String? = null,
    @JsonProperty("ShippingQuantity_Box") val shippingQuantityBox: String? = null,
    @JsonProperty("ShippingQuantity") val shippingQuantity: String? = null,
    @JsonProperty("OrderSalesLineAmount") val orderSalesLineAmount: String? = null,
    @JsonProperty("ShippingAmount") val shippingAmount: String? = null,
    @JsonProperty("Plant") val plant: String? = null,
    @JsonProperty("Plant_NM") val plantNm: String? = null,
    @JsonProperty("ReleaseQuantity") val releaseQuantity: String? = null,
    @JsonProperty("ReleaseAmount") val releaseAmount: String? = null
)
