package com.otoki.powersales.external.sap.inbound.dto.order

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP ERP 주문 인바운드 요청 DTO. (Spec #561)
 *
 * 페이로드 키는 SAP RESTAdapter 호환을 위해 레거시 Apex 필드명(`reqItemList`) 을 그대로 수신한다.
 */
data class ErpOrderRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<ErpOrderRequestItem>?
)
