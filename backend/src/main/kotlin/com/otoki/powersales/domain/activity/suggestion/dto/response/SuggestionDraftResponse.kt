package com.otoki.powersales.domain.activity.suggestion.dto.response

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionDraft

/**
 * 제안 임시저장 조회/저장 응답 DTO.
 *
 * 제안 등록 폼 prefill 용. 거래처명/제품명은 등록 요청에 없으므로 함께 내려준다.
 * 사진은 저장된 S3 key 를 presigned URL 로 변환해 내려준다(없으면 null). 최대 2장.
 */
data class SuggestionDraftResponse(
    val category: String?,
    val title: String?,
    val content: String?,
    val productCode: String?,
    val productName: String?,
    val accountId: Long?,
    val accountName: String?,
    val sapAccountCode: String?,
    val claimType: String?,
    val claimDate: String?,
    val carNumber: String?,
    val logisticsResponsibility: String?,
    val duplicateProposalNum: String?,
    val actionStatus: String?,
    val photoUrls: List<String>
) {
    companion object {
        /**
         * @param urlResolver S3 key → presigned URL 변환기 (key 부재 시 null 반환)
         */
        fun from(draft: SuggestionDraft, urlResolver: (String?) -> String?): SuggestionDraftResponse =
            SuggestionDraftResponse(
                category = draft.category,
                title = draft.title,
                content = draft.content,
                productCode = draft.productCode,
                productName = draft.productName,
                accountId = draft.accountId,
                accountName = draft.accountName,
                sapAccountCode = draft.sapAccountCode,
                claimType = draft.claimType,
                claimDate = draft.claimDate?.toString(),
                carNumber = draft.carNumber,
                logisticsResponsibility = draft.logisticsResponsibility,
                duplicateProposalNum = draft.duplicateProposalNum,
                actionStatus = draft.actionStatus,
                photoUrls = listOfNotNull(
                    urlResolver(draft.photoKey1),
                    urlResolver(draft.photoKey2)
                )
            )
    }
}
