package com.otoki.powersales.external.sap.inbound.dto.appointment

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 인사발령 행 DTO. (Spec #562)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (레거시 `IF_REST_SAP_Appointment.ReqItem` 동일).
 *
 * 단, `Employeecode` 는 레거시 원본 선언(`IF_REST_SAP_Appointment.cls:29 public String Employeecode;`)이
 * **소문자 c** 다. SAP 는 이 키 그대로 송신하며, 신규는 Jackson 기본 case-sensitive 바인딩이므로
 * `@JsonProperty` 키를 레거시와 동일한 `Employeecode` 로 맞춘다 (대문자 `EmployeeCode` 로 두면 미바인딩 → null).
 */
data class AppointmentRequestItem(
    @JsonProperty("Employeecode") val employeeCode: String? = null,
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
