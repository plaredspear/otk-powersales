package com.otoki.powersales.sap.inbound.dto.appointment

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 인사발령 인바운드 요청 DTO. (Spec #562)
 */
data class AppointmentRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<AppointmentRequestItem>?
)
