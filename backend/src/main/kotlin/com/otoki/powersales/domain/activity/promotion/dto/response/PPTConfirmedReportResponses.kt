package com.otoki.powersales.domain.activity.promotion.dto.response

/**
 * 전문행사조 확정 인원 보고서 응답 (Spec #846).
 *
 * 레거시 매핑: SF Report `new_report_swJ` (전문행사조 확정 인원·Tabular 6컬럼·Confirmed__c=1).
 */
data class PPTConfirmedReportResponse(
    val items: List<PPTConfirmedReportItem>,
)

/**
 * 확정 인원 1행 (6컬럼) — ProfessionalPromotionTeamMaster × employee × account.
 *
 * professionalPromotionTeam 은 teamType displayName (한국어).
 */
data class PPTConfirmedReportItem(
    val branchName: String?,
    val fullName: String?,
    val employeeNumber: String?,
    val accountName: String?,
    val accountCode: String?,
    val professionalPromotionTeam: String?,
)
