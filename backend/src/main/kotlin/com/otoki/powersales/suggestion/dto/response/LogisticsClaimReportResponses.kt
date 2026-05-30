package com.otoki.powersales.suggestion.dto.response

/**
 * (영업본부) 물류 클레임 보고서 응답 (Spec #844).
 *
 * 레거시 매핑: SF Report `OLS_dmK`(기간별) + `new_report_6dy`(당월) + `OLS_NDx`(전월).
 * 세 보고서는 컬럼 동일, 기간 프리셋(period)만 차이. category='물류 클레임' + claimDate 기간 필터. 전사.
 */
data class LogisticsClaimReportResponse(
    val period: String,
    val startDate: String,
    val endDate: String,
    val items: List<LogisticsClaimReportItem>,
)

/**
 * 물류 클레임 1행 — suggestion × employee × account × product × owner(User) 조인 (22컬럼).
 *
 * custName = 레코드 Owner User 이름 (SF CUST_NAME 의사 컬럼).
 */
data class LogisticsClaimReportItem(
    val custName: String?,
    val createdDate: String?,
    val claimDate: String?,
    val responsibleLogisticsCenter: String?,
    val logisticsResponsibility: String?,
    val claimType: String?,
    val title: String?,
    val content: String?,
    val productCode: String?,
    val productName: String?,
    val productCategory: String?,
    val branchName: String?,
    val accountCode: String?,
    val accountName: String?,
    val orgName: String?,
    val employeeCode: String?,
    val employeeName: String?,
    val jikwee: String?,
    val jobCode: String?,
    val carNumber: String?,
    val actionStatus: String?,
    val actionContent: String?,
    val duplicateProposalNum: String?,
)
