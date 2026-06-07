package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.common.entity.UploadFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

data class AdminClaimListResponse(
    val content: List<AdminClaimListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AdminClaimListItem(
    val claimId: Long,
    val employeeName: String?,
    val employeeCode: String?,
    val storeName: String?,
    val productName: String?,
    val productCode: String?,
    val categoryValue: String?,
    val categoryLabel: String?,
    val subcategoryValue: String?,
    val subcategoryLabel: String?,
    val defectQuantity: BigDecimal?,
    val status: String,
    val createdAt: LocalDateTime,
    /**
     * 목록 카드 뷰 배경용 대표 이미지 URL.
     * 불량(CLAIM_DEFECT) 사진 우선, 없으면 첫 사진. 사진이 없으면 null.
     */
    val representativeImageUrl: String?
) {
    companion object {
        fun from(claim: Claim, representativeImageUrl: String?): AdminClaimListItem = AdminClaimListItem(
            claimId = claim.id,
            employeeName = claim.employee?.name,
            employeeCode = claim.employee?.employeeCode,
            storeName = claim.account?.name,
            productName = claim.product?.name,
            productCode = claim.product?.productCode,
            categoryValue = claim.claimType1.value,
            categoryLabel = claim.claimType1.label,
            subcategoryValue = claim.claimType2.value,
            subcategoryLabel = claim.claimType2.label,
            defectQuantity = claim.defectQuantity,
            status = claim.status.name,
            createdAt = claim.createdAt,
            representativeImageUrl = representativeImageUrl
        )
    }
}

data class AdminClaimDetailResponse(
    val claimId: Long,
    val employeeName: String?,
    val employeeCode: String?,
    val storeName: String?,
    val productCode: String?,
    val productName: String?,
    val dateType: String?,
    val date: LocalDate?,
    val categoryValue: String?,
    val categoryLabel: String?,
    val subcategoryValue: String?,
    val subcategoryLabel: String?,
    val defectDescription: String?,
    val defectQuantity: BigDecimal?,
    val purchaseAmount: BigDecimal?,
    val purchaseMethodName: String?,
    val requestTypeName: String?,
    val status: String,
    val createdAt: LocalDateTime,
    val photos: List<ClaimPhotoResponse>
) {
    companion object {
        fun from(
            claim: Claim,
            uploadFiles: List<UploadFile>,
            urlResolver: (String?) -> String?
        ): AdminClaimDetailResponse = AdminClaimDetailResponse(
            claimId = claim.id,
            employeeName = claim.employee?.name,
            employeeCode = claim.employee?.employeeCode,
            storeName = claim.account?.name,
            productCode = claim.product?.productCode,
            productName = claim.product?.name,
            dateType = claim.dateType?.name,
            date = claim.date,
            categoryValue = claim.claimType1.value,
            categoryLabel = claim.claimType1.label,
            subcategoryValue = claim.claimType2.value,
            subcategoryLabel = claim.claimType2.label,
            defectDescription = claim.defectDescription,
            defectQuantity = claim.defectQuantity,
            purchaseAmount = claim.purchaseAmount,
            purchaseMethodName = claim.purchaseMethodCode?.displayName,
            requestTypeName = claim.requestTypeCode.joinToString(";") { it.displayName }.ifBlank { null },
            status = claim.status.name,
            createdAt = claim.createdAt,
            photos = uploadFiles.mapNotNull { ClaimPhotoResponse.from(it, urlResolver) }
        )
    }
}

/**
 * 클레임 첨부 이미지 응답.
 *
 * 데이터 소스: UploadFile (SF UploadFile__c 마이그레이션 entity).
 * - photoType: SF UploadFile__c 에 분류 (클레임/일부인/영수증) 필드가 없으므로 null. UI 에서 분류 태그 미표시.
 * - url: UploadFile.uniqueKey (= S3 객체 key) 를 presigned URL 로 변환 (private/ 저장, 인증 기반 조회).
 *   resolver 가 null 을 반환하면 (uniqueKey 부재) 응답에서 제외.
 */
data class ClaimPhotoResponse(
    val photoId: Long,
    val photoType: String?,
    val url: String,
    val originalFileName: String?
) {
    companion object {
        fun from(uploadFile: UploadFile, urlResolver: (String?) -> String?): ClaimPhotoResponse? {
            val resolved = urlResolver(uploadFile.uniqueKey) ?: return null
            return ClaimPhotoResponse(
                photoId = uploadFile.id,
                photoType = null,
                url = resolved,
                originalFileName = uploadFile.name
            )
        }
    }
}
