package com.otoki.powersales.inspection.dto.response

import com.otoki.powersales.inspection.entity.SiteActivityDraft

/**
 * 현장점검 임시저장 조회/저장 응답 DTO (등록 폼 prefill 용).
 *
 * 테마/현장유형/분류는 코드(value)만 내려주고 화면이 목록으로 이름을 해석한다.
 * 거래처명/제품명은 form-data 에 없으므로 함께 내려준다.
 * 사진은 저장된 S3 key 를 public URL 로 변환해 순서대로 내려준다(없으면 빈 리스트).
 */
data class SiteActivityDraftResponse(
    val themeId: Long?,
    val category: String?,
    val accountId: Long?,
    val accountName: String?,
    val inspectionDate: String?,
    val fieldTypeCode: String?,
    val description: String?,
    val productCode: String?,
    val productName: String?,
    val competitorName: String?,
    val competitorActivity: String?,
    val competitorTasting: Boolean?,
    val competitorProductName: String?,
    val competitorProductPrice: Int?,
    val competitorSalesQuantity: Int?,
    val photoUrls: List<String>
) {
    companion object {
        /**
         * @param urlResolver S3 key → public URL 변환기 (key 부재 시 null 반환)
         */
        fun from(draft: SiteActivityDraft, urlResolver: (String?) -> String?): SiteActivityDraftResponse =
            SiteActivityDraftResponse(
                themeId = draft.themeId,
                category = draft.category,
                accountId = draft.accountId,
                accountName = draft.accountName,
                inspectionDate = draft.inspectionDate?.toString(),
                fieldTypeCode = draft.fieldTypeCode,
                description = draft.description,
                productCode = draft.productCode,
                productName = draft.productName,
                competitorName = draft.competitorName,
                competitorActivity = draft.competitorActivity,
                competitorTasting = draft.competitorTasting,
                competitorProductName = draft.competitorProductName,
                competitorProductPrice = draft.competitorProductPrice,
                competitorSalesQuantity = draft.competitorSalesQuantity,
                photoUrls = listOfNotNull(draft.photoKey1, draft.photoKey2)
                    .mapNotNull { urlResolver(it) }
            )
    }
}
