package com.otoki.powersales.external.sap.inbound.dto.product

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 제품 마스터 인바운드 요청 DTO. (Spec #559)
 */
data class ProductMasterRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    @JsonAlias("ReqItemList", "reqitemlist", "REQITEMLIST", "REQ_ITEM_LIST")
    val reqItemList: List<ProductMasterRequestItem>?
)
