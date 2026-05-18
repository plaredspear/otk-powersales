package com.otoki.powersales.sf.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SF 월 매출 이력 행 DTO (Spec #775).
 *
 * UPSERT 키 externalKey = SAPAccountCode + SalesYearMonth (YYYYMM).
 *
 * 도메인 [com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand] 와 1:1 매핑.
 * SAP 측 [com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequestItem] 와 동일 필드.
 */
data class SfMonthlySalesHistoryRequestItem(
    @JsonProperty("SAPAccountCode") val sapAccountCode: String? = null,
    @JsonProperty("SalesYearMonth") val salesYearMonth: String? = null,
    @JsonProperty("ABCClosingAmount1") val abcClosingAmount1: String? = null,
    @JsonProperty("ABCClosingAmount2") val abcClosingAmount2: String? = null,
    @JsonProperty("ABCClosingAmount3") val abcClosingAmount3: String? = null,
    @JsonProperty("TotalLedgerAmount") val totalLedgerAmount: String? = null,
    @JsonProperty("ShipClosingAmount") val shipClosingAmount: String? = null,
    @JsonProperty("rlsales") val rlsales: String? = null
)
