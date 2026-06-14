package com.otoki.powersales.external.sap.inbound.dto.organization

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SAP 조직 마스터 인바운드 요청 DTO.
 * SAP RESTAdapter 호환을 위해 키는 PascalCase / UPPER_CASE 그대로 수신한다 (#556 spec).
 */
data class OrganizationMasterRequest(
    @field:NotNull(message = "reqItemList 는 필수입니다")
    @JsonProperty("reqItemList")
    val reqItemList: List<OrganizationMasterRequestItem>?
)
