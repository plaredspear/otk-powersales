package com.otoki.powersales.sap.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 월 매출 이력 행 DTO. (Spec #560)
 *
 * UPSERT 키 externalKey = SAPAccountCode + SalesYearMonth (YYYYMM).
 */
data class MonthlySalesHistoryRequestItem(
    @JsonProperty("SAPAccountCode") val sapAccountCode: String? = null,
    @JsonProperty("SalesYearMonth") val salesYearMonth: String? = null,
    @JsonProperty("ABCClosingAmount1") val abcClosingAmount1: String? = null,
    @JsonProperty("ABCClosingAmount2") val abcClosingAmount2: String? = null,
    @JsonProperty("ABCClosingAmount3") val abcClosingAmount3: String? = null,
    @JsonProperty("TotalLedgerAmount") val totalLedgerAmount: String? = null,
    @JsonProperty("ShipClosingAmount") val shipClosingAmount: String? = null,
    @JsonProperty("rlsales") val rlsales: String? = null
)
