package com.otoki.powersales.domain.activity.inspection.dto.request

/**
 * 현장점검 임시저장 요청 DTO (multipart 의 "request" 파트, application/json).
 *
 * 임시저장은 검증을 건너뛰므로 모든 필드가 nullable 이다(레거시 tempFieldChkProc 정합).
 * 거래처명/제품명은 prefill 표시용으로 함께 전달받아 저장한다(코드만으로는 이름 복원이 불가).
 * 사진은 컨트롤러에서 "photos" 파트로 별도 수신한다.
 */
data class SiteActivityDraftRequest(
    val themeId: Long? = null,
    val category: String? = null,
    val accountId: Long? = null,
    val accountName: String? = null,
    val inspectionDate: String? = null,
    val fieldTypeCode: String? = null,
    val description: String? = null,
    val productCode: String? = null,
    val productName: String? = null,
    val competitorName: String? = null,
    val competitorActivity: String? = null,
    val competitorTasting: Boolean? = null,
    val competitorProductName: String? = null,
    val competitorProductPrice: Int? = null,
    val competitorSalesQuantity: Int? = null
)
