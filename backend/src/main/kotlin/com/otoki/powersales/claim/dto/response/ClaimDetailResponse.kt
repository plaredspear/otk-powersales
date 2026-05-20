package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.ClaimPhoto
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
        fun from(claim: Claim, photos: List<ClaimPhoto>): ClaimDetailResponse = ClaimDetailResponse(
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
            photos = photos.map { ClaimPhotoItem.from(it) }
        )
    }
}

data class ClaimPhotoItem(
    val photoId: Long,
    val photoType: String,
    val photoTypeLabel: String,
    val url: String,
    val originalFileName: String?
) {
    companion object {
        fun from(photo: ClaimPhoto): ClaimPhotoItem = ClaimPhotoItem(
            photoId = photo.id,
            photoType = photo.photoType.name,
            photoTypeLabel = photo.photoType.label,
            url = photo.url,
            originalFileName = photo.originalFileName
        )
    }
}
