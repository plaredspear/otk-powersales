package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotResendableException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * SF 재전송 service — Spec #829.
 *
 * 사전 조건: claim.status == SEND_FAILED (그 외 상태는 409).
 * 처리: S3 이미지 회수 → SF push (AdminClaimCreateService 의 헬퍼 재사용) → status update.
 */
@Service
class AdminClaimResendService(
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val createService: AdminClaimCreateService,
    private val txTemplate: TransactionTemplate,
) {

    fun resend(claimId: Long): AdminClaimCreateResponse {
        val snapshot = txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(claimId)
                ?: throw ClaimNotFoundException(claimId)
            if (claim.status != ClaimStatus.SEND_FAILED) {
                throw ClaimNotResendableException()
            }
            val photos = uploadFileRepository
                .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimId)
            val defect = photos.first { it.uploadKbn == UploadFileKbnTypes.CLAIM_DEFECT }
            val label = photos.first { it.uploadKbn == UploadFileKbnTypes.CLAIM_PART }
            val receipt = photos.firstOrNull { it.uploadKbn == UploadFileKbnTypes.CLAIM_RECEIPT }

            ResendSnapshot(
                claimId = claim.id,
                employeeCode = claim.employee?.employeeCode
                    ?: error("재전송 시점에 claim.employee 가 null"),
                sapAccountCode = claim.account?.externalKey
                    ?: error("재전송 시점에 claim.account.externalKey 가 null"),
                productCode = claim.product?.productCode
                    ?: error("재전송 시점에 claim.product.productCode 가 null"),
                parsed = AdminClaimCreateService.ParsedInput(
                    sapAccountCode = claim.account?.externalKey!!,
                    productCode = claim.product?.productCode!!,
                    employeeCode = claim.employee?.employeeCode
                        ?: error("사번 미보유 사원의 claim 은 재전송할 수 없습니다"),
                    dateType = claim.dateType ?: ClaimDateType.EXPIRY_DATE,
                    date = claim.date,
                    claimDate = claim.createdAt.toLocalDate(),
                    claimType1 = claim.claimType1,
                    claimType2 = claim.claimType2,
                    quantity = claim.defectQuantity,
                    description = claim.defectDescription,
                    purchaseMethod = claim.purchaseMethodCode,
                    amount = claim.purchaseAmount,
                    requestTypes = claim.requestTypeCode,
                ),
                claimKey = defect.uniqueKey ?: error("DEFECT 사진 uniqueKey 누락"),
                claimFilename = defect.name ?: "claim",
                claimContentType = "image/jpeg",
                partKey = label.uniqueKey ?: error("PART 사진 uniqueKey 누락"),
                partFilename = label.name ?: "part",
                partContentType = "image/jpeg",
                receiptKey = receipt?.uniqueKey,
                receiptFilename = receipt?.name,
                receiptContentType = receipt?.let { "image/jpeg" },
            )
        }!!

        // SF call (트랜잭션 외부)
        val sfResult = createService.pushToSfFromStored(
            employeeCode = snapshot.employeeCode,
            sapAccountCode = snapshot.sapAccountCode,
            productCode = snapshot.productCode,
            parsed = snapshot.parsed,
            claimKey = snapshot.claimKey,
            partKey = snapshot.partKey,
            receiptKey = snapshot.receiptKey,
            claimPhotoFilename = snapshot.claimFilename,
            claimPhotoContentType = snapshot.claimContentType,
            partPhotoFilename = snapshot.partFilename,
            partPhotoContentType = snapshot.partContentType,
            receiptPhotoFilename = snapshot.receiptFilename,
            receiptPhotoContentType = snapshot.receiptContentType,
        )

        // Transaction 2 — status update
        return txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(snapshot.claimId)
                ?: throw ClaimNotFoundException(snapshot.claimId)
            createService.applySfResultToClaim(claim, sfResult)
            AdminClaimCreateResponse(
                claimId = claim.id,
                status = claim.status.name,
                sfResultCode = sfResult.apiResponse?.resultCode,
                sfResultMsg = sfResult.apiResponse?.resultMsg ?: sfResult.errorSummary,
            )
        }!!
    }

    private data class ResendSnapshot(
        val claimId: Long,
        val employeeCode: String,
        val sapAccountCode: String,
        val productCode: String,
        val parsed: AdminClaimCreateService.ParsedInput,
        val claimKey: String,
        val claimFilename: String,
        val claimContentType: String,
        val partKey: String,
        val partFilename: String,
        val partContentType: String,
        val receiptKey: String?,
        val receiptFilename: String?,
        val receiptContentType: String?,
    )
}
