package com.otoki.powersales.external.sap.inbound.dto.account

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 거래처 마스터 행 DTO. (Spec #558)
 *
 * 페이로드 키는 SAP 호환을 위해 PascalCase 로 유지한다 (`@JsonProperty` 명시 바인딩).
 * 페이로드 36 키 중 `SAPAccountCode` / `Name` 은 entity 생성자에 전달되며, 나머지는
 * [com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertCommand] 를 거쳐
 * [com.otoki.powersales.domain.foundation.account.service.AccountUpsertMapper] 가 entity mutable 필드에 적재한다.
 * 일부 필드(`AccountStatusCode` / `SalesDeptCode` / `DivisionCode`) 는 spec #602 (sf-annotate-account)
 * 가 SF 정합 목적으로 entity 컬럼을 도입했고, spec #646 이 SAP 인바운드 매핑(Org 매칭 결과 적재)을 추가했다.
 * spec #649 (account-business-number-source-realign) 은 `BusinessLicenseNumber` 가 SF Sic 매핑 컬럼
 * `business_number` 가 아닌 별도 `business_license_number` 컬럼에 적재되는 데이터 source 차이의 정합 회복을 다룬다.
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
