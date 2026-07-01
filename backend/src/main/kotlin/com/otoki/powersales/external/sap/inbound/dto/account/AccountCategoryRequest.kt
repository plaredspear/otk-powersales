package com.otoki.powersales.external.sap.inbound.dto.account

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 거래처 카테고리 마스터 인바운드 요청 DTO. (Spec #558)
 */
data class AccountCategoryRequest(
    @field:NotNull(message = "reqItemList 는 필수입니다")
    @JsonProperty("reqItemList")
    @JsonAlias("ReqItemList", "reqitemlist", "REQITEMLIST", "REQ_ITEM_LIST")
    val reqItemList: List<AccountCategoryRequestItem>?
)

data class AccountCategoryRequestItem(
    @JsonProperty("AccountCode") val accountCode: String? = null,
    @JsonProperty("Name") val name: String? = null
)
