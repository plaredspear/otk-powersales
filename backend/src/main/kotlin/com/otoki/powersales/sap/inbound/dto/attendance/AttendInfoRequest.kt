package com.otoki.powersales.sap.inbound.dto.attendance

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 출근 정보 인바운드 요청 DTO. (Spec #562)
 */
data class AttendInfoRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<AttendInfoRequestItem>?
)
