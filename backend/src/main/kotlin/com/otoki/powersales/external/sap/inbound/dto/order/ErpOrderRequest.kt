package com.otoki.powersales.external.sap.inbound.dto.order

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP ERP 주문 인바운드 요청 DTO. (Spec #561)
 *
 * 페이로드 키는 SAP RESTAdapter 호환을 위해 레거시 Apex 필드명(`reqItemList`) 을 그대로 수신한다.
 *
 * 레거시 SF `JSON.deserializeStrict` 는 키 대소문자를 구분하지 않아 SAP 가 `ReqItemList` /
 * `reqitemlist` 등으로 보내도 매핑에 성공했다. Jackson 은 기본적으로 키 대소문자를 엄격히
 * 구분하므로, 레거시 동등성 유지를 위해 [JsonAlias] 로 대소문자 변형을 흡수한다.
 * (canonical 키는 [JsonProperty] `reqItemList` — 직렬화 출력에 사용.)
 */
data class ErpOrderRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    @JsonAlias("ReqItemList", "reqitemlist", "REQITEMLIST", "REQ_ITEM_LIST")
    val reqItemList: List<ErpOrderRequestItem>?
)
