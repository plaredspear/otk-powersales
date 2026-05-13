package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.ClaimPhoto
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminClaimListResponse(
    val content: List<AdminClaimListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AdminClaimListItem(
    val claimId: Long,
    val employeeName: String,
    val employeeCode: String,
    val storeName: String?,
    val productName: String?,
    val productCode: String?,
    val categoryValue: String?,
    val categoryLabel: String?,
    val subcategoryValue: String?,
    val subcategoryLabel: String?,
    val defectQuantity: Int?,
    val status: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(claim: Claim): AdminClaimListItem = AdminClaimListItem(
            claimId = claim.id,
            employeeName = claim.employee.name,
            employeeCode = claim.employee.employeeCode,
            storeName = claim.accountName,
            productName = claim.productName,
            productCode = claim.productCode,
            categoryValue = claim.claimType1.value,
            categoryLabel = claim.claimType1.label,
            subcategoryValue = claim.claimType2.value,
            subcategoryLabel = claim.claimType2.label,
            defectQuantity = claim.defectQuantity,
            status = claim.status.name,
            createdAt = claim.createdAt
        )
    }
}

data class AdminClaimDetailResponse(
    val claimId: Long,
    val employeeName: String,
    val employeeCode: String,
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
    val defectQuantity: Int?,
    val purchaseAmount: Int?,
    val purchaseMethodName: String?,
    val requestTypeName: String?,
    val status: String,
    val createdAt: LocalDateTime,
    val photos: List<ClaimPhotoResponse>
) {
    companion object {
        fun from(claim: Claim, photos: List<ClaimPhoto>): AdminClaimDetailResponse = AdminClaimDetailResponse(
            claimId = claim.id,
            employeeName = claim.employee.name,
            employeeCode = claim.employee.employeeCode,
            storeName = claim.accountName,
            productCode = claim.productCode,
            productName = claim.productName,
            dateType = claim.dateType.name,
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
            createdAt = claim.createdAt,
            photos = photos.map { ClaimPhotoResponse.from(it) }
        )
    }
}

data class ClaimPhotoResponse(
    val photoId: Long,
    val photoType: String,
    val url: String,
    val originalFileName: String?
) {
    companion object {
        fun from(photo: ClaimPhoto): ClaimPhotoResponse = ClaimPhotoResponse(
            photoId = photo.id,
            photoType = photo.photoType.name,
            url = photo.url,
            originalFileName = photo.originalFileName
        )
    }
}
