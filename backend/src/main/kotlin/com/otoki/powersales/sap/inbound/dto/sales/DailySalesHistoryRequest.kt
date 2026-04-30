package com.otoki.powersales.sap.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 일 매출 이력 인바운드 요청 DTO. (Spec #560)
 */
data class DailySalesHistoryRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<DailySalesHistoryRequestItem>?
)
