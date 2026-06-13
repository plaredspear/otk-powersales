package com.otoki.powersales.external.sap.inbound.dto.attendance

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 출근 정보 행 DTO. (Spec #562)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (레거시 `IF_REST_SAP_AttendInfo.ReqItem` 동일).
 */
data class AttendInfoRequestItem(
    @JsonProperty("EmployeeCode") val employeeCode: String? = null,
    @JsonProperty("StartDate") val startDate: String? = null,
    @JsonProperty("EndDate") val endDate: String? = null,
    @JsonProperty("AttendType") val attendType: String? = null,
    @JsonProperty("Status") val status: String? = null
)
