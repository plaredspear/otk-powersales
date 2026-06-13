package com.otoki.powersales.external.sap.inbound.dto.product

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 제품 바코드 마스터 인바운드 요청 DTO. (Spec #559)
 */
data class BarcodeMasterRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<BarcodeMasterRequestItem>?
)
