package com.otoki.powersales.sap.inbound.dto.employee

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 직원 마스터 인바운드 요청 DTO. (Spec #557)
 *
 * 페이로드 키는 SAP RESTAdapter 호환을 위해 레거시 Apex 필드명(`reqItemList`) 을 그대로 수신한다 —
 * 글로벌 SNAKE_CASE 정책의 예외이며 `@JsonProperty` 로 명시 바인딩한다.
 */
data class EmployeeMasterRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<EmployeeMasterRequestItem>?
)
