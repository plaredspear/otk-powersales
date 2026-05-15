package com.otoki.powersales.sap.inbound.dto.claim

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 클레임 상태 인바운드 요청 DTO. (Spec #561)
 *
 * 레거시는 `Input.request: ReqItem` (단건) 구조이지만, 신규 인터페이스는 일관성을 위해
 * `reqItemList` (배열) 형태로 받는다. 단일 호출에 1건만 보내는 것도 허용.
 */
data class ClaimStatusRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<ClaimStatusRequestItem>?
)
