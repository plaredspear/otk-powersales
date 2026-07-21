package com.otoki.powersales.external.sap.inbound.dto.order

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP ERP 주문 헤더 행 DTO. (Spec #561)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다.
 * 라인은 [ItemDetailList] 로 헤더에 종속된다.
 */
data class ErpOrderRequestItem(
    @JsonProperty("SAPOrderNumber") val sapOrderNumber: String? = null,
    @JsonProperty("RefSAPOrderNumber") val refSapOrderNumber: String? = null,
    @JsonProperty("SAPAccountCode") val sapAccountCode: String? = null,
    @JsonProperty("SAPAccountName") val sapAccountName: String? = null,
    @JsonProperty("DeliveryRequestDate") val deliveryRequestDate: String? = null,
    @JsonProperty("OrderDate") val orderDate: String? = null,
    @JsonProperty("EmployeeCode") val employeeCode: String? = null,
    @JsonProperty("EmployeeName") val employeeName: String? = null,
    @JsonProperty("OrderSalesAmount") val orderSalesAmount: String? = null,
    @JsonProperty("OrderChannel") val orderChannel: String? = null,
    @JsonProperty("OrderChannel_NM") val orderChannelNm: String? = null,
    @JsonProperty("OrderType") val orderType: String? = null,
    @JsonProperty("OrderType_NM") val orderTypeNm: String? = null,
    @JsonProperty("ItemDetailList") val itemDetailList: List<ErpOrderItemDetail>? = null
)
