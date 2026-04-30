package com.otoki.powersales.sap.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 일 매출 이력 행 DTO. (Spec #560)
 *
 * UPSERT 키 externalKey = SAPAccountCode + SalesDate (YYYYMMDD).
 */
data class DailySalesHistoryRequestItem(
    @JsonProperty("SAPAccountCode") val sapAccountCode: String? = null,
    @JsonProperty("SalesDate") val salesDate: String? = null,
    @JsonProperty("ERPSalesAmount1") val erpSalesAmount1: String? = null,
    @JsonProperty("ERPSalesAmount2") val erpSalesAmount2: String? = null,
    @JsonProperty("ERPSalesAmount3") val erpSalesAmount3: String? = null,
    @JsonProperty("ERPDistributionAmount1") val erpDistributionAmount1: String? = null,
    @JsonProperty("ERPDistributionAmount2") val erpDistributionAmount2: String? = null,
    @JsonProperty("ERPDistributionAmount3") val erpDistributionAmount3: String? = null,
    @JsonProperty("LedgerAmount") val ledgerAmount: String? = null
)
