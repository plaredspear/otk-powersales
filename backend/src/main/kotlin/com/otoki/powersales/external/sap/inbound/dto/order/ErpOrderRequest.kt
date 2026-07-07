package com.otoki.powersales.external.sap.inbound.dto.order

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP ERP 주문 인바운드 요청 DTO. (Spec #561)
 *
 * 페이로드 키는 SAP RESTAdapter 호환을 위해 레거시 Apex 필드명(`reqItemList`) 을 그대로 수신한다.
 *
 * 레거시 SF `JSON.deserializeStrict` 는 키 대소문자를 구분하지 않아 SAP 가 `ReqItemList` /
 * `reqitemlist` 등으로 보내도 매핑에 성공했다. Jackson 은 기본적으로 키 대소문자를 엄격히
 * 구분하므로, 레거시 동등성 유지를 위해 [JsonAlias] 로 대소문자 변형을 흡수한다.
 * (canonical 키는 [JsonProperty] `reqItemList` — 직렬화 출력에 사용.)
 *
 * 주의 — `reqItemList` 는 필수 검증(@NotNull)을 두지 않는다. SAP 가 alias 세트에 없는 키 표기로
 * 보내면 `reqItemList=null` 로 바인딩되는데, 이때 400/422 로 거절하지 않고 적재 0건 200 으로 흘려보낸다
 * (SapErpOrderController 참고). 이는 SAP 측이 실패를 받지 않게 하기 위한 임시 조치이며,
 * SAP 가 실제 전송하는 키 표기가 확인되면 [JsonAlias] 에 추가해 정상 바인딩(주문 적재)되도록 해야 한다.
 * 키 표기 진단은 REQUEST_REJECTED_PAYLOAD audit 로그로 가능하나, null 을 흘려보내는 현재 정책에서는
 * 400/422 가 발생하지 않으므로 해당 audit 도 남지 않는 점에 유의.
 */
data class ErpOrderRequest(
    @JsonProperty("reqItemList")
    @JsonAlias("ReqItemList", "reqitemlist", "REQITEMLIST", "REQ_ITEM_LIST")
    val reqItemList: List<ErpOrderRequestItem>?
)
