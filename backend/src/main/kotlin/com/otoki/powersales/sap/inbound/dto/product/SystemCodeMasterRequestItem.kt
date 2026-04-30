package com.otoki.powersales.sap.inbound.dto.product

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 시스템 공통 코드 마스터 행 DTO. (Spec #559)
 *
 * UPSERT 키 externalKey 는 `CompanyCode + ';' + GroupCode + ';' + DetailCode`.
 */
data class SystemCodeMasterRequestItem(
    @JsonProperty("CompanyCode") val companyCode: String? = null,
    @JsonProperty("GroupCode") val groupCode: String? = null,
    @JsonProperty("DetailCode") val detailCode: String? = null,
    @JsonProperty("GroupCodeName") val groupCodeName: String? = null,
    @JsonProperty("DetailCodeName") val detailCodeName: String? = null,
    @JsonProperty("SEQ") val seq: String? = null
)
