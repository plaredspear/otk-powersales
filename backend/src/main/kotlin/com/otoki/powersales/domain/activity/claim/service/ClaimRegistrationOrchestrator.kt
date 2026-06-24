package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile

/**
 * 클레임 등록 트랜잭션 오케스트레이터 — web ([AdminClaimCreateService]) · mobile ([MobileClaimService]) 공용.
 *
 * 진입점이 입력 검증·entity 조회·S3 업로드를 끝낸 뒤 본 오케스트레이터를 호출하면 다음 3단 시퀀스를 조율한다:
 *   1. [Transaction 1] claim + photo INSERT (status=SF_PENDING)
 *   2. [SF call] ClaimSfOutboundService.pushToSf("/ClaimRegist") — 트랜잭션 외부
 *   3. [Transaction 2] status update (성공 → SENT, 실패 → SEND_FAILED)
 *
 * SF 호출은 트랜잭션 외부에서 일어나며 실패해도 throw 하지 않는다 — claim 은 SEND_FAILED 로 보존되어
 * 나중에 [AdminClaimResendService] 로 수동 재전송 가능. 본 오케스트레이터는 채널/응답 매핑에 무관하며,
 * 등록 후 [RegistrationResult] (claim + SF 결과)를 반환해 진입점이 각자의 응답 DTO 로 매핑한다.
 *
 * 이렇게 Tx 골격·repository·SF I/O 의존을 본 클래스에 가둬, 진입점([MobileClaimService]/[AdminClaimCreateService])은
 * 입력 처리에만 집중하고 트랜잭션/INSERT 변경의 영향 범위가 본 클래스 한 곳에 머문다.
 *
 * 트랜잭션 경계를 [txTemplate] 으로 직접 관리하므로 본 클래스에 클래스 레벨 @Transactional 을 두지 않는다.
 */
@Service
class ClaimRegistrationOrchestrator(
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val sfOutboundService: ClaimSfOutboundService,
    private val txTemplate: TransactionTemplate,
) {

    /**
     * 검증·조회·S3 업로드를 마친 입력으로 클레임을 등록하고 SF dual-write 를 수행한다.
     *
     * @param parsed 검증 완료된 SF 전송 입력 (진입점별 파싱 결과)
     * @param channel 등록 경로 (web=WEB, mobile=CAP)
     * @param photos 업로드 완료된 사진 (defect/part 필수, receipt 선택)
     * @param onAfterInsert Tx1 내부에서 INSERT 직후 실행할 후처리 (예: mobile draft 삭제). 트랜잭션에 포함된다.
     * @return 상태 전이까지 완료된 [Claim] 과 SF push 결과 ([RegistrationResult])
     */
    // 트랜잭션 경계는 txTemplate 으로 직접 관리한다 — 진입 시점에 트랜잭션(특히 readOnly)이 있으면
    // 내부 txTemplate(REQUIRED) 이 거기 참여해 INSERT 가 막힌다. NEVER 로 상위 트랜잭션을 능동 차단.
    @Transactional(propagation = Propagation.NEVER)
    fun register(
        employee: Employee,
        account: Account,
        product: Product,
        parsed: ClaimSfOutboundService.ParsedInput,
        channel: ClaimChannel,
        photos: ClaimPhotos,
        onAfterInsert: () -> Unit = {},
    ): RegistrationResult {
        // 1. Transaction 1 — DB INSERT (status=SF_PENDING)
        val savedClaim = txTemplate.execute {
            val claim = Claim(
                employee = employee,
                account = account,
                dateType = parsed.dateType,
                date = parsed.date,
                claimType1 = parsed.claimType1,
                claimType2 = parsed.claimType2,
                defectDescription = parsed.description,
                defectQuantity = parsed.quantity,
                purchaseAmount = parsed.amount,
                purchaseMethodCode = parsed.purchaseMethod,
                requestTypeCode = parsed.requestTypes,
                status = ClaimStatus.SF_PENDING,
                channel = channel,
                product = product,
                // SF ClaimRegist.cls:90 정합 — CC코드는 거래처(Account) BranchCode 기준 (CostCenter__c formula = AccountId__r.BranchCode__c 와 동일).
                costCenterCode = account.branchCode,
                // SF 정합 — division__c 는 IF_REST_MOBILE_ClaimRegist 컨트롤러가 set 안 함 + 트리거는 Interface 가드로 미실행 → 등록 시 공란.
                division = null,
            )
            val saved = claimRepository.save(claim)
            val rows = mutableListOf<UploadFile>().apply {
                add(buildPhoto(saved, photos.defectPhoto, photos.defectKey, UploadFileKbnTypes.CLAIM_DEFECT))
                add(buildPhoto(saved, photos.partPhoto, photos.partKey, UploadFileKbnTypes.CLAIM_PART))
                if (photos.receiptPhoto != null && photos.receiptKey != null) {
                    add(buildPhoto(saved, photos.receiptPhoto, photos.receiptKey, UploadFileKbnTypes.CLAIM_RECEIPT))
                }
            }
            uploadFileRepository.saveAll(rows)
            onAfterInsert()
            saved
        }!!

        // 2. SF call (트랜잭션 외부)
        val sfResult = sfOutboundService.pushToSf(
            employeeCode = parsed.employeeCode,
            sapAccountCode = parsed.sapAccountCode,
            productCode = parsed.productCode,
            parsed = parsed,
            channel = channel.name,
            claimPhoto = photos.defectPhoto,
            claimKey = photos.defectKey,
            partPhoto = photos.partPhoto,
            partKey = photos.partKey,
            receiptPhoto = photos.receiptPhoto,
            receiptKey = photos.receiptKey,
        )

        // 3. Transaction 2 — status update
        val updatedClaim = txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(savedClaim.id)
                ?: throw ClaimNotFoundException(savedClaim.id)
            sfOutboundService.applySfResultToClaim(claim, sfResult)
            claim
        }!!
        return RegistrationResult(updatedClaim, sfResult)
    }

    private fun buildPhoto(claim: Claim, file: MultipartFile, key: String, uploadKbn: String): UploadFile =
        UploadFile(
            name = file.originalFilename ?: "unknown",
            uniqueKey = key,
            fileSize = file.size.toString(),
            parentType = UploadFileParentTypes.CLAIM,
            parentId = claim.id,
            uploadKbn = uploadKbn,
            isDeleted = false,
        )

    /** 등록 결과 — 상태 전이 완료된 claim + SF push 결과. */
    data class RegistrationResult(
        val claim: Claim,
        val sfResult: ClaimSfOutboundService.SfPushResult,
    )

    /**
     * 업로드 완료된 클레임 사진 (defect/part 필수, receipt 선택) + S3 key.
     * part == 모바일 "label(라벨)" 사진 == SF payload 의 PartImage* (uploadKbn=CLAIM_PART).
     */
    data class ClaimPhotos(
        val defectPhoto: MultipartFile,
        val defectKey: String,
        val partPhoto: MultipartFile,
        val partKey: String,
        val receiptPhoto: MultipartFile?,
        val receiptKey: String?,
    )
}
