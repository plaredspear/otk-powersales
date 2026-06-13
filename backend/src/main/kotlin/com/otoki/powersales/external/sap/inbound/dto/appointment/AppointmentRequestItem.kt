package com.otoki.powersales.external.sap.inbound.dto.appointment

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 인사발령 행 DTO. (Spec #562)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (레거시 `IF_REST_SAP_Appointment.ReqItem` 동일).
 */
data class AppointmentRequestItem(
    @JsonProperty("EmployeeCode") val employeeCode: String? = null,
    @JsonProperty("AfterOrgCode") val afterOrgCode: String? = null,
    @JsonProperty("AfterOrgName") val afterOrgName: String? = null,
    @JsonProperty("Jikchak") val jikchak: String? = null,
    @JsonProperty("Jikwee") val jikwee: String? = null,
    @JsonProperty("Jikgub") val jikgub: String? = null,
    @JsonProperty("WorkType") val workType: String? = null,
    @JsonProperty("ManageType") val manageType: String? = null,
    @JsonProperty("JobCode") val jobCode: String? = null,
    @JsonProperty("WorkArea") val workArea: String? = null,
    @JsonProperty("Jikjong") val jikjong: String? = null,
    @JsonProperty("AppointDate") val appointDate: String? = null,
    @JsonProperty("JobName") val jobName: String? = null,
    @JsonProperty("OrdDetailCode") val ordDetailCode: String? = null,
    @JsonProperty("OrdDetailNode") val ordDetailNode: String? = null
)
