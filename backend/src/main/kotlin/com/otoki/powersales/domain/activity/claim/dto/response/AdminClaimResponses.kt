package com.otoki.powersales.domain.activity.claim.dto.response

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.platform.common.entity.UploadFile
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
    // status : SF DKRetail__Status__c (코스모스 전송상태) — 표시 전용.
    val status: String?,
    // sfSendStatus : 신규→SF 전송상태. SF origin 마이그레이션 건은 null. 재전송 필요 건(SEND_FAILED) 식별용.
    val sfSendStatus: String?,
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
            categoryValue = claim.claimType1?.value,
            categoryLabel = claim.claimType1?.label,
            subcategoryValue = claim.claimType2?.value,
            subcategoryLabel = claim.claimType2?.label,
            defectQuantity = claim.defectQuantity,
            status = claim.status?.name,
            sfSendStatus = claim.sfSendStatus?.name,
            createdAt = claim.createdAt,
            representativeImageUrl = representativeImageUrl
        )
    }
}

/**
 * 클레임 상세 응답 (web admin).
 *
 * 표시 항목은 레거시 SF 클레임 상세(알라딘) 화면의 섹션 — 제품정보 / Information / 불만 정보 /
 * 채널정보 / 처리·조치 정보 — 전 항목을 매핑한다 (모바일 [ClaimDetailResponse] 와 동일 범위).
 * SF formula(파생) 필드(제품코드/거래처지점명/직위/주문번호 등)는 §6.7 정책에 따라 재현하지 않고,
 * 이미 보유한 FK join 값(사번·제품코드·연락처)으로 대체한다.
 */
data class AdminClaimDetailResponse(
    val claimId: Long,
    // 제품정보
    val productCode: String?,
    val productName: String?,
    val manufacturingDate: LocalDate?,
    val logisticsCenter: String?,
    val expirationDate: LocalDate?,
    // SF OrderNumber__c formula (= DKRetail__ReturnOrderNumber__c 재노출).
    val orderNumber: String?,
    // Information (클레임정보)
    val claimNo: String?,
    val storeName: String?,
    val categoryValue: String?,
    val categoryLabel: String?,
    val subcategoryValue: String?,
    val subcategoryLabel: String?,
    val defectQuantity: BigDecimal?,
    val sampleCollectionFlag: Boolean?,
    // status : SF DKRetail__Status__c (코스모스 전송상태) — 표시 전용.
    val status: String?,
    // sfSendStatus/sfSendStatusLabel : 신규→SF 전송상태 (ClaimSfSendStatus). 재전송 버튼 판정 축.
    // SF origin 마이그레이션 건은 null (재전송 대상 아님).
    val sfSendStatus: String?,
    val sfSendStatusLabel: String?,
    val customerDeliveryDate: LocalDate?,
    val detailSnsName: String?,
    val dateType: String?,
    val date: LocalDate?,
    val purchaseMethodName: String?,
    val purchaseAmount: BigDecimal?,
    val requestTypeName: String?,
    val division: String?,
    // 불만 정보
    val defectDescription: String?,
    // 채널정보
    val interfaceDate: LocalDateTime?,
    val channel: String?,
    val channelLabel: String?,
    val employeeName: String?,
    val employeeCode: String?,
    val employeePhone: String?,
    // SF Jikwee__c formula (= DKRetail__EmployeeId__r.DKRetail__JobCode__c).
    val jikwee: String?,
    // 처리·조치 정보
    val counselNumber: String?,
    val actionCode: String?,
    val actionStatus: String?,
    val reasonType: String?,
    val actContent: String?,
    // 메타
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
            // 제품정보
            productCode = claim.product?.productCode,
            productName = claim.product?.name,
            manufacturingDate = claim.manufacturingDate,
            logisticsCenter = claim.logisticsCenter,
            expirationDate = claim.expirationDate,
            // SF OrderNumber__c = DKRetail__ReturnOrderNumber__c 재노출 formula 정합.
            orderNumber = claim.returnOrderNumber,
            // Information (클레임정보)
            claimNo = claim.name,
            storeName = claim.account?.name,
            categoryValue = claim.claimType1?.value,
            categoryLabel = claim.claimType1?.label,
            subcategoryValue = claim.claimType2?.value,
            subcategoryLabel = claim.claimType2?.label,
            defectQuantity = claim.defectQuantity,
            sampleCollectionFlag = claim.sampleCollectionFlag,
            status = claim.status?.name,
            sfSendStatus = claim.sfSendStatus?.name,
            sfSendStatusLabel = claim.sfSendStatus?.displayName,
            customerDeliveryDate = claim.customerDeliveryDate,
            detailSnsName = claim.detailSnsName,
            dateType = claim.dateType?.name,
            date = claim.date,
            purchaseMethodName = claim.purchaseMethodCode?.displayName,
            purchaseAmount = claim.purchaseAmount,
            requestTypeName = claim.requestTypeCode.joinToString(";") { it.displayName }.ifBlank { null },
            division = claim.division,
            // 불만 정보
            defectDescription = claim.defectDescription,
            // 채널정보
            interfaceDate = claim.interfaceDate,
            channel = claim.channel?.name,
            channelLabel = claim.channel?.displayName,
            employeeName = claim.employee?.name,
            employeeCode = claim.employee?.employeeCode,
            employeePhone = claim.employee?.phone,
            // SF Jikwee__c = Employee.JobCode 파생 formula 정합.
            jikwee = claim.employee?.jobCode,
            // 처리·조치 정보
            counselNumber = claim.counselNumber,
            actionCode = claim.actionCode,
            actionStatus = claim.actionStatus,
            reasonType = claim.reasonType,
            actContent = claim.actContent,
            // 메타
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
