package com.otoki.powersales.claim.dto.request

import java.math.BigDecimal

/**
 * 클레임 임시저장 요청 DTO (multipart/form-data, @ModelAttribute).
 *
 * 임시저장은 검증을 건너뛰므로 모든 필드가 nullable 이다(레거시 tempClaimProc 정합).
 * 거래처명/제품명은 prefill 표시용으로 함께 전달받아 저장한다(form-data 에 없는 정보).
 * 사진(defectPhoto/labelPhoto/receiptPhoto)은 컨트롤러에서 @RequestParam 으로 별도 수신한다.
 */
data class ClaimDraftRequest(
    val accountId: Long? = null,
    val accountName: String? = null,
    val productCode: String? = null,
    val productName: String? = null,
    val dateType: String? = null,
    val date: String? = null,
    val claimType1: String? = null,
    val claimType2: String? = null,
    val defectDescription: String? = null,
    val defectQuantity: BigDecimal? = null,
    val purchaseAmount: BigDecimal? = null,
    val purchaseMethodCode: String? = null,
    val requestTypeCode: String? = null
)
