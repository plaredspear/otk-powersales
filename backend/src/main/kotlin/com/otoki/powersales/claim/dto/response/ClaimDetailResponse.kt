package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.common.entity.UploadFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

data class ClaimDetailResponse(
    val claimId: Long,
    val accountName: String?,
    val productName: String?,
    val productCode: String?,
    val dateType: String?,
    val dateTypeLabel: String?,
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
    val statusLabel: String,
    val createdAt: LocalDateTime,
    val photos: List<ClaimPhotoItem>
) {
    companion object {
        fun from(
            claim: Claim,
            photos: List<UploadFile>,
            urlResolver: (String?) -> String?
        ): ClaimDetailResponse = ClaimDetailResponse(
            claimId = claim.id,
            accountName = claim.accountName,
            productName = claim.productName,
            productCode = claim.productCode,
            dateType = claim.dateType?.name,
            dateTypeLabel = claim.dateType?.label,
            date = claim.date,
            categoryValue = claim.claimType1.value,
            categoryLabel = claim.claimType1.label,
            subcategoryValue = claim.claimType2.value,
            subcategoryLabel = claim.claimType2.label,
            defectDescription = claim.defectDescription,
            defectQuantity = claim.defectQuantity,
            purchaseAmount = claim.purchaseAmount,
            purchaseMethodName = claim.purchaseMethodName,
            requestTypeName = claim.requestTypeName,
            status = claim.status.name,
            statusLabel = claim.status.displayName,
            createdAt = claim.createdAt,
            photos = photos.mapNotNull { ClaimPhotoItem.from(it, urlResolver) }
        )
    }
}

/**
 * 클레임 첨부 이미지 응답.
 *
 * 데이터 소스: UploadFile (SF UploadFile__c 마이그레이션 entity).
 * - url: UploadFile.uniqueKey (= S3 객체 key) 를 PublicUrlResolver 가 완전 URL 로 변환.
 *   resolver 가 null 을 반환하면 (uniqueKey 부재) 응답에서 제외.
 */
data class ClaimPhotoItem(
    val photoId: Long,
    val url: String,
    val originalFileName: String?
) {
    companion object {
        fun from(uploadFile: UploadFile, urlResolver: (String?) -> String?): ClaimPhotoItem? {
            val resolved = urlResolver(uploadFile.uniqueKey) ?: return null
            return ClaimPhotoItem(
                photoId = uploadFile.id,
                url = resolved,
                originalFileName = uploadFile.name
            )
        }
    }
}
