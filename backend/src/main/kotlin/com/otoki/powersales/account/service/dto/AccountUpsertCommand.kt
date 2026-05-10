package com.otoki.powersales.account.service.dto

/**
 * 거래처 UPSERT 도메인 입력 커맨드.
 *
 * 외부 채널(SAP 인바운드 / 어드민 일괄 등록 등) 의 페이로드를 [AccountUpsertService] 가 받기 위한 도메인 용어 모델.
 * 외부 키 명명(`SAPAccountCode`, `BranchCode`, `SalesDeptCode` 등) 및 `@JsonProperty` 가 침투하지 않는다.
 *
 * - [externalKey] : SAP 거래처 코드 (= [com.otoki.powersales.account.entity.Account.externalKey])
 * - [branchCode] / [salesDeptCode] / [divisionCode] : Organization 폴백 lookup 우선순위 키
 */
data class AccountUpsertCommand(
    val externalKey: String?,
    val name: String?,
    val accountType: String?,
    val accountStatusCode: String?,
    val accountStatusName: String?,
    val accountGroup: String?,
    val phone: String?,
    val mobilePhone: String?,
    val email: String?,
    val businessType: String?,
    val businessCategory: String?,
    val employeeCode: String?,
    val businessLicenseNumber: String?,
    val representative: String?,
    val zipcode: String?,
    val address1: String?,
    val address2: String?,
    val divisionCode: String?,
    val divisionName: String?,
    val salesDeptCode: String?,
    val salesDeptName: String?,
    val branchCode: String?,
    val branchName: String?,
    val closingTime1: String?,
    val closingTime2: String?,
    val closingTime3: String?,
    val abcType: String?,
    val abcTypeCode: String?,
    val distribution: String?,
    val consignmentAcc: String?,
    val werk1: String?,
    val werk2: String?,
    val werk3: String?,
    val werk1Tx: String?,
    val werk2Tx: String?,
    val werk3Tx: String?
)
