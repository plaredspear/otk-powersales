package com.otoki.powersales.sap.inbound.dto.account

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 거래처 마스터 행 DTO. (Spec #558)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (`@JsonProperty` 명시 바인딩).
 * 신규 시스템 [com.otoki.powersales.account.entity.Account] 에 매핑되지 않는 필드(예:
 * AccountStatusCode / SalesDeptCode / DivisionCode 등)는 수신은 하되 저장 컬럼이 없어
 * 무시된다 — 레거시 매핑 전체 13~30 필드 중 실제 컬럼이 있는 항목만 기록된다.
 */
data class ClientMasterRequestItem(
    @JsonProperty("SAPAccountCode") val sapAccountCode: String? = null,
    @JsonProperty("AccountType") val accountType: String? = null,
    @JsonProperty("Name") val name: String? = null,
    @JsonProperty("AccountStatusCode") val accountStatusCode: String? = null,
    @JsonProperty("AccountStatusName") val accountStatusName: String? = null,
    @JsonProperty("AccountGroup") val accountGroup: String? = null,
    @JsonProperty("Phone") val phone: String? = null,
    @JsonProperty("MobilePhone") val mobilePhone: String? = null,
    @JsonProperty("Email") val email: String? = null,
    @JsonProperty("BusinessType") val businessType: String? = null,
    @JsonProperty("BusinessCategory") val businessCategory: String? = null,
    @JsonProperty("EmployeeCode") val employeeCode: String? = null,
    @JsonProperty("BusinessLicenseNumber") val businessLicenseNumber: String? = null,
    @JsonProperty("Representative") val representative: String? = null,
    @JsonProperty("Zipcode") val zipcode: String? = null,
    @JsonProperty("Address1") val address1: String? = null,
    @JsonProperty("Address2") val address2: String? = null,
    @JsonProperty("DivisionCode") val divisionCode: String? = null,
    @JsonProperty("DivisionName") val divisionName: String? = null,
    @JsonProperty("SalesDeptCode") val salesDeptCode: String? = null,
    @JsonProperty("SalesDeptName") val salesDeptName: String? = null,
    @JsonProperty("BranchCode") val branchCode: String? = null,
    @JsonProperty("BranchName") val branchName: String? = null,
    @JsonProperty("ClosingTime1") val closingTime1: String? = null,
    @JsonProperty("ClosingTime2") val closingTime2: String? = null,
    @JsonProperty("ClosingTime3") val closingTime3: String? = null,
    @JsonProperty("ABCType") val abcType: String? = null,
    @JsonProperty("ABCTypeCode") val abcTypeCode: String? = null,
    @JsonProperty("Distribution") val distribution: String? = null,
    @JsonProperty("ConsignmentAcc") val consignmentAcc: String? = null,
    @JsonProperty("WERK1") val werk1: String? = null,
    @JsonProperty("WERK2") val werk2: String? = null,
    @JsonProperty("WERK3") val werk3: String? = null,
    @JsonProperty("WERK1_TX") val werk1Tx: String? = null,
    @JsonProperty("WERK2_TX") val werk2Tx: String? = null,
    @JsonProperty("WERK3_TX") val werk3Tx: String? = null
)
