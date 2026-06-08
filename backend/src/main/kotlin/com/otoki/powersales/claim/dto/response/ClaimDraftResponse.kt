package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.ClaimDraft

/**
 * 클레임 임시저장 조회/저장 응답 DTO.
 *
 * 클레임 등록 폼 prefill 용. 종류1/2·구매방법·요청사항은 코드(value)만 내려주고
 * 화면이 form-data 로 이름을 해석한다. 거래처명/제품명은 form-data 에 없으므로 함께 내려준다.
 * 사진은 저장된 S3 key 를 presigned URL 로 변환해 내려준다(없으면 null).
 */
data class ClaimDraftResponse(
    val accountId: Long?,
    val accountName: String?,
    val productCode: String?,
    val productName: String?,
    val dateType: String?,
    val date: String?,
    val claimType1: String?,
    val claimType2: String?,
    val defectDescription: String?,
    val defectQuantity: Long?,
    val purchaseAmount: Long?,
    val purchaseMethodCode: String?,
    val requestTypeCode: String?,
    val defectPhotoUrl: String?,
    val labelPhotoUrl: String?,
    val receiptPhotoUrl: String?
) {
    companion object {
        /**
         * @param urlResolver S3 key → presigned URL 변환기 (key 부재 시 null 반환)
         */
        fun from(draft: ClaimDraft, urlResolver: (String?) -> String?): ClaimDraftResponse =
            ClaimDraftResponse(
                accountId = draft.accountId,
                accountName = draft.accountName,
                productCode = draft.productCode,
                productName = draft.productName,
                dateType = draft.dateType,
                date = draft.claimDate?.toString(),
                claimType1 = draft.claimType1,
                claimType2 = draft.claimType2,
                defectDescription = draft.defectDescription,
                defectQuantity = draft.defectQuantity?.toLong(),
                purchaseAmount = draft.purchaseAmount?.toLong(),
                purchaseMethodCode = draft.purchaseMethodCode,
                requestTypeCode = draft.requestTypeCode,
                defectPhotoUrl = urlResolver(draft.defectPhotoKey),
                labelPhotoUrl = urlResolver(draft.labelPhotoKey),
                receiptPhotoUrl = urlResolver(draft.receiptPhotoKey)
            )
    }
}
