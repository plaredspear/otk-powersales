package com.otoki.powersales.domain.activity.claim.dto.response

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

/**
 * 클레임 상세 응답 (모바일).
 *
 * 표시 항목은 레거시 Heroku `claim/view.jsp` 의 5개 섹션(제품정보 / 클레임정보 / 불만정보 /
 * 채널정보 / 처리·조치정보) + 사진을 기준으로 전 항목을 매핑한다.
 */
data class ClaimDetailResponse(
    val claimId: Long,
    // 제품정보
    val productName: String? = null,
    val productCode: String? = null,
    val manufacturingDate: LocalDate? = null,
    val logisticsCenter: String? = null,
    val expirationDate: LocalDate? = null,
    // 클레임정보
    val claimNo: String? = null,
    val accountName: String? = null,
    val accountCode: String? = null,
    val categoryValue: String? = null,
    val categoryLabel: String? = null,
    val subcategoryValue: String? = null,
    val subcategoryLabel: String? = null,
    val defectQuantity: BigDecimal? = null,
    val sampleCollectionFlag: Boolean? = null,
    val status: String?,
    val statusLabel: String?,
    val customerDeliveryDate: LocalDate? = null,
    val detailSnsName: String? = null,
    val dateType: String? = null,
    val dateTypeLabel: String? = null,
    val date: LocalDate? = null,
    val purchaseMethodName: String? = null,
    val purchaseAmount: BigDecimal? = null,
    val requestTypeName: String? = null,
    val division: String? = null,
    // 불만정보
    val defectDescription: String? = null,
    // 채널정보
    val interfaceDate: LocalDateTime? = null,
    val channel: String? = null,
    val channelLabel: String? = null,
    val employeeName: String? = null,
    val employeePhone: String? = null,
    // 처리·조치정보
    val counselNumber: String? = null,
    val actionCode: String? = null,
    val actionStatus: String? = null,
    val reasonType: String? = null,
    val actContent: String? = null,
    // 메타
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
            // 제품정보
            productName = claim.product?.name,
            productCode = claim.product?.productCode,
            manufacturingDate = claim.manufacturingDate,
            logisticsCenter = claim.logisticsCenter,
            expirationDate = claim.expirationDate,
            // 클레임정보
            claimNo = claim.name,
            accountName = claim.account?.name,
            accountCode = claim.account?.externalKey,
            categoryValue = claim.claimType1?.value,
            categoryLabel = claim.claimType1?.label,
            subcategoryValue = claim.claimType2?.value,
            subcategoryLabel = claim.claimType2?.label,
            defectQuantity = claim.defectQuantity,
            sampleCollectionFlag = claim.sampleCollectionFlag,
            status = claim.status?.name,
            statusLabel = claim.status?.displayName,
            customerDeliveryDate = claim.customerDeliveryDate,
            detailSnsName = claim.detailSnsName,
            dateType = claim.dateType?.name,
            dateTypeLabel = claim.dateType?.label,
            date = claim.date,
            purchaseMethodName = claim.purchaseMethodCode?.displayName,
            purchaseAmount = claim.purchaseAmount,
            requestTypeName = claim.requestTypeCode.joinToString(";") { it.displayName }.ifBlank { null },
            division = claim.division,
            // 불만정보
            defectDescription = claim.defectDescription,
            // 채널정보
            interfaceDate = claim.interfaceDate,
            channel = claim.channel?.name,
            channelLabel = claim.channel?.displayName,
            employeeName = claim.employee?.name,
            employeePhone = claim.employee?.phone,
            // 처리·조치정보
            counselNumber = claim.counselNumber,
            actionCode = claim.actionCode,
            actionStatus = claim.actionStatus,
            reasonType = claim.reasonType,
            actContent = claim.actContent,
            // 메타
            createdAt = claim.createdAt,
            photos = photos.mapNotNull { ClaimPhotoItem.from(it, urlResolver) }
        )
    }
}

/**
 * 클레임 첨부 이미지 응답.
 *
 * 데이터 소스: UploadFile (SF UploadFile__c 마이그레이션 entity).
 * - url: UploadFile.uniqueKey (= S3 객체 key) 를 presigned URL 로 변환 (private/ 저장, 인증 기반 조회).
 *   resolver 가 null 을 반환하면 (uniqueKey 부재) 응답에서 제외.
 * - photoType: UploadFile.uploadKbn (claim/part/receipt) 을 모바일 분류값(DEFECT/LABEL/RECEIPT) 으로 매핑.
 *   모바일 상세 화면이 불량/일부인/영수증 섹션 분류에 사용. 미지정 시 null.
 */
data class ClaimPhotoItem(
    val photoId: Long,
    val photoType: String? = null,
    val url: String,
    val originalFileName: String?
) {
    companion object {
        fun from(uploadFile: UploadFile, urlResolver: (String?) -> String?): ClaimPhotoItem? {
            val resolved = urlResolver(uploadFile.uniqueKey) ?: return null
            return ClaimPhotoItem(
                photoId = uploadFile.id,
                photoType = toPhotoType(uploadFile.uploadKbn),
                url = resolved,
                originalFileName = uploadFile.name
            )
        }

        private fun toPhotoType(uploadKbn: String?): String? = when (uploadKbn) {
            UploadFileKbnTypes.CLAIM_DEFECT -> "DEFECT"
            UploadFileKbnTypes.CLAIM_PART -> "LABEL"
            UploadFileKbnTypes.CLAIM_RECEIPT -> "RECEIPT"
            else -> null
        }
    }
}
