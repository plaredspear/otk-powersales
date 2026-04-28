package com.otoki.powersales.sap.inbound.dto.account

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 거래처 마스터 인바운드 요청 DTO. (Spec #558)
 * SAP RESTAdapter 호환을 위해 키는 `req_item_list` (snake_case) 로 수신한다.
 */
data class ClientMasterRequest(
    @field:NotNull(message = "req_item_list 는 필수입니다")
    @JsonProperty("req_item_list")
    val reqItemList: List<ClientMasterRequestItem>?
)
