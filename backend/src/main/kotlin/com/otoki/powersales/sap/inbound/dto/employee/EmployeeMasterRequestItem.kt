package com.otoki.powersales.sap.inbound.dto.employee

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 직원 마스터 행 DTO. (Spec #557)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (`@JsonProperty` 명시 바인딩).
 * 13개 필드는 레거시 `IF_REST_SAP_EmployeeMaster.ReqItem` 과 동일하다.
 */
data class EmployeeMasterRequestItem(
    @JsonProperty("EmployeeCode") val employeeCode: String? = null,
    @JsonProperty("EmployeeName") val employeeName: String? = null,
    @JsonProperty("Sex") val gender: String? = null,
    @JsonProperty("HomePhone") val homePhone: String? = null,
    @JsonProperty("WorkPhone") val workPhone: String? = null,
    @JsonProperty("WorkEmail") val workEmail: String? = null,
    @JsonProperty("Email") val email: String? = null,
    @JsonProperty("StartDate") val startDate: String? = null,
    @JsonProperty("EndDate") val endDate: String? = null,
    @JsonProperty("Status") val status: String? = null,
    @JsonProperty("Birthdate") val birthdate: String? = null,
    @JsonProperty("OrgCode") val orgCode: String? = null,
    @JsonProperty("LockingFlag") val lockingFlag: String? = null
)
