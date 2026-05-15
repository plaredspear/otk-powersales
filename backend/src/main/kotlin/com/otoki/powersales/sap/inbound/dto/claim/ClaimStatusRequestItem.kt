package com.otoki.powersales.sap.inbound.dto.claim

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 클레임 상태 행 DTO. (Spec #561)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (`@JsonProperty` 명시 바인딩).
 */
data class ClaimStatusRequestItem(
    @JsonProperty("Name") val name: String? = null,
    @JsonProperty("ClaimSequence") val claimSequence: String? = null,
    @JsonProperty("ActionCode") val actionCode: String? = null,
    @JsonProperty("ClaimStatus") val claimStatus: String? = null,
    @JsonProperty("Content") val content: String? = null,
    @JsonProperty("ReasonType") val reasonType: String? = null,
    @JsonProperty("COSMOSKey") val cosmosKey: String? = null
)
