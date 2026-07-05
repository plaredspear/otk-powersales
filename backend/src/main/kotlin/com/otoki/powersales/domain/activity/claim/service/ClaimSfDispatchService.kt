package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * 클레임 SF push 디스패치 — 신규 등록(이벤트) · 수동 재전송 공용.
 *
 * claimId 하나로 DB 에서 claim + S3 이미지 좌표를 복원해 SF `/ClaimRegist` 로 전송하고 status 를 전이한다.
 * 등록 직후 비동기 전송([ClaimSfPushDispatcher])과 실패 후 수동 재전송([AdminClaimResendService])이
 * 동일한 "snapshot 복원 → [ClaimSfOutboundService.pushToSfFromStored] → status update" 경로를 공유한다.
 *
 * 입력 이미지는 항상 S3 에서 다시 읽는다 — 등록 트랜잭션 커밋 후(@Async) 또는 한참 뒤 재전송 시점엔
 * HTTP 요청의 MultipartFile 이 무효화되므로, 등록 시 이미 업로드해둔 S3 객체가 유일한 안정적 출처다.
 *
 * SF 호출은 트랜잭션 외부에서 일어나며 실패해도 throw 하지 않는다 — claim 은 SEND_FAILED 로 보존되어
 * [AdminClaimResendService] 로 재전송 가능하다.
 */
@Service
class ClaimSfDispatchService(
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val sfOutboundService: ClaimSfOutboundService,
    private val txTemplate: TransactionTemplate,
) {

    /**
     * claimId 기준으로 SF 전송을 수행하고 status 를 전이한다.
     *
     * @param allowedStatuses 전송 허용 상태. 등록 직후 트리거는 [ClaimStatus.SF_PENDING],
     *   수동 재전송은 [ClaimStatus.SEND_FAILED] 만 허용한다. 그 외 상태면 [onStatusMismatch] 위임.
     * @param onStatusMismatch 상태 가드 위반 시 동작 (재전송은 409 throw, 등록 트리거는 skip 로깅).
     * @return SF push 결과 + 전이 완료된 claim 상태 ([DispatchResult]). 가드 위반으로 skip 시 null.
     */
    fun dispatch(
        claimId: Long,
        allowedStatuses: Set<ClaimStatus>,
        onStatusMismatch: (ClaimStatus?) -> Unit,
    ): DispatchResult? {
        // 1. 트랜잭션 안에서 claim + 이미지 좌표 복원 (snapshot).
        val snapshot = txTemplate.execute {
            // employee/account/product 를 fetch join 으로 즉시 로드 — @ManyToOne(LAZY) 프록시가
            // enhancement 환경에서 미초기화되어 null 로 평가되는 문제 회피 (findByIdWithSfRefs KDoc 참조).
            val claim = claimRepository.findByIdWithSfRefs(claimId)
                ?: throw ClaimNotFoundException(claimId)
            if (claim.status !in allowedStatuses) {
                onStatusMismatch(claim.status)
                return@execute null
            }
            val photos = uploadFileRepository
                .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimId)
            val defect = photos.first { it.uploadKbn == UploadFileKbnTypes.CLAIM_DEFECT }
            val label = photos.first { it.uploadKbn == UploadFileKbnTypes.CLAIM_PART }
            val receipt = photos.firstOrNull { it.uploadKbn == UploadFileKbnTypes.CLAIM_RECEIPT }

            ResendSnapshot(
                claimId = claim.id,
                employeeCode = claim.employee?.employeeCode
                    ?: error("전송 시점에 claim.employee 가 null"),
                sapAccountCode = claim.account?.externalKey
                    ?: error("전송 시점에 claim.account.externalKey 가 null"),
                productCode = claim.product?.productCode
                    ?: error("전송 시점에 claim.product.productCode 가 null"),
                // 등록 경로별 channel 유지 (web=WEB, mobile=CAP). 미설정 row 는 WEB 으로 fallback.
                channel = (claim.channel ?: ClaimChannel.WEB).name,
                parsed = ClaimSfOutboundService.ParsedInput(
                    sapAccountCode = claim.account?.externalKey!!,
                    productCode = claim.product?.productCode!!,
                    employeeCode = claim.employee?.employeeCode
                        ?: error("사번 미보유 사원의 claim 은 전송할 수 없습니다"),
                    dateType = claim.dateType ?: ClaimDateType.EXPIRY_DATE,
                    date = claim.date ?: error("발생일자 미보유 claim 은 전송할 수 없습니다"),
                    claimDate = claim.createdAt.toLocalDate(),
                    claimType1 = claim.claimType1 ?: error("클레임대분류 미보유 claim 은 전송할 수 없습니다"),
                    claimType2 = claim.claimType2 ?: error("클레임소분류 미보유 claim 은 전송할 수 없습니다"),
                    quantity = claim.defectQuantity ?: error("수량 미보유 claim 은 전송할 수 없습니다"),
                    description = claim.defectDescription ?: error("불만내용 미보유 claim 은 전송할 수 없습니다"),
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
        } ?: return null

        // 2. SF call (트랜잭션 외부) — S3 이미지를 다시 읽어 전송.
        val sfResult = sfOutboundService.pushToSfFromStored(
            // 대상 claim PK 를 pwrskey 로 전송 — SF 역연결용.
            pwrskey = snapshot.claimId,
            employeeCode = snapshot.employeeCode,
            sapAccountCode = snapshot.sapAccountCode,
            productCode = snapshot.productCode,
            parsed = snapshot.parsed,
            channel = snapshot.channel,
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

        // 3. Transaction 2 — status update (SENT / SEND_FAILED).
        val finalStatus = txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(snapshot.claimId)
                ?: throw ClaimNotFoundException(snapshot.claimId)
            sfOutboundService.applySfResultToClaim(claim, sfResult)
            claim.status
        }
        return DispatchResult(sfResult = sfResult, status = finalStatus)
    }

    /** SF push 결과 + 전이 완료된 claim status. */
    data class DispatchResult(
        val sfResult: ClaimSfOutboundService.SfPushResult,
        val status: ClaimStatus?,
    )

    private data class ResendSnapshot(
        val claimId: Long,
        val employeeCode: String,
        val sapAccountCode: String,
        val productCode: String,
        val channel: String,
        val parsed: ClaimSfOutboundService.ParsedInput,
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
