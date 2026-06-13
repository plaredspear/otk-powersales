package com.otoki.powersales.domain.activity.claim.dto.response

import java.math.BigDecimal

/**
 * 기간별 클레임 보고서 응답 (Spec #843).
 *
 * 레거시 매핑: SF Report `X3_ONLY_veg` (포장불량만·21컬럼) + `X4_3xv` (모든 클레임·23컬럼).
 * type=PACKAGING(포장불량만) / ALL(모든 클레임) 분기. status='전송완료' + ClaimDate 기간 필터.
 * totalQuantity = 조회 결과 수량 합계 (레거시 Quantity Sum + showGrandTotal).
 */
data class ClaimPeriodReportResponse(
    val startDate: String,
    val endDate: String,
    val type: String,
    val totalQuantity: BigDecimal,
    val items: List<ClaimPeriodReportItem>,
)

/**
 * 기간별 클레임 1행 — claim × employee × account × product 조인.
 *
 * 23컬럼 공통 DTO. PACKAGING(21컬럼) 일 때 claimType1/detailSnsName 은 응답에 포함되나 프론트에서 숨김.
 * enum 필드는 한국어 label/displayName.
 */
data class ClaimPeriodReportItem(
    val claimName: String?,
    val interfaceDate: String?,
    val claimDate: String?,
    val claimType1: String?,
    val branchName: String?,
    val employeeCode: String?,
    val employeeName: String?,
    val mobilePhone: String?,
    val accountName: String?,
    val detailSnsName: String?,
    val externalKey: String?,
    val productName: String?,
    val productCode: String?,
    val manufacturingDate: String?,
    val expirationDate: String?,
    val quantity: BigDecimal?,
    val claimType2: String?,
    val defectDescription: String?,
    val counselNumber: String?,
    val actionStatus: String?,
    val actionCode: String?,
    val reasonType: String?,
    val actContent: String?,
)
