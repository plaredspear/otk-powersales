package com.otoki.powersales.external.sap.inbound.dto.appointment

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 인사발령 행 DTO. (Spec #562)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (레거시 `IF_REST_SAP_Appointment.ReqItem` 동일).
 *
 * 단, `Employeecode` 는 레거시 원본 선언(`IF_REST_SAP_Appointment.cls:29 public String Employeecode;`)이
 * **소문자 c** 다. SAP 는 이 키 그대로 송신하며, Jackson 은 case-sensitive 바인딩이므로 레거시 소문자 key 를
 * `@JsonAlias("Employeecode")` 로 수용한다. 동시에 정상 PascalCase `EmployeeCode` 도 `@JsonProperty` 로 받아
 * 양쪽 모두 사번이 유실되지 않도록 한다.
 */
data class AppointmentRequestItem(
    // 레거시 SF IF_REST_SAP_Appointment 의 실제 수신 key 는 `Employeecode`(끝 code 소문자).
    // Apex 는 member 접근이 case-insensitive 라 둘 다 동작했으나 Jackson 은 case-sensitive 이므로
    // 정상 PascalCase + 레거시 소문자 둘 다 수용한다 (사번 유실 방지).
    @JsonProperty("EmployeeCode")
    @JsonAlias("Employeecode")
    val employeeCode: String? = null,
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
