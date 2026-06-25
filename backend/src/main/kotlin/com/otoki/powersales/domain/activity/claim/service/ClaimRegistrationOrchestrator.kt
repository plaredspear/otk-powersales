package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile

/**
 * 클레임 등록 트랜잭션 오케스트레이터 — web ([AdminClaimCreateService]) · mobile ([MobileClaimService]) 공용.
 *
 * 진입점이 입력 검증·entity 조회·S3 업로드를 끝낸 뒤 본 오케스트레이터를 호출하면 다음을 조율한다:
 *   1. [Transaction] claim + photo INSERT (status=SF_PENDING) + onAfterInsert
 *   2. 커밋 후 SF 송신 트리거 — [ClaimRegisteredEvent] 발행 (수신: [ClaimSfPushDispatcher])
 *
 * SF 호출은 본 오케스트레이터에서 일어나지 않는다. 등록 트랜잭션 커밋 후 [ClaimSfPushDispatcher] 가
 * 비동기(@Async)로 [ClaimSfDispatchService.dispatch] 를 통해 전송하고 status 를 전이(SENT/SEND_FAILED)한다.
 * SF 실패해도 claim 은 SEND_FAILED 로 보존되어 [AdminClaimResendService] 로 수동 재전송 가능하다.
 *
 * 진입점은 등록된 [Claim] (status=SF_PENDING) 만 동기로 응답받는다 — SF 전송 결과는 즉시 확인하지 않고,
 * web 상세 화면의 상태 배너/재전송 버튼으로 확인한다.
 *
 * 이렇게 Tx 골격·repository 의존을 본 클래스에, SF I/O 를 디스패처/[ClaimSfDispatchService] 에 가둬,
 * 진입점([MobileClaimService]/[AdminClaimCreateService])은 입력 처리에만 집중한다.
 *
 * 트랜잭션 경계를 [txTemplate] 으로 직접 관리하므로 본 클래스에 클래스 레벨 @Transactional 을 두지 않는다.
 */
@Service
class ClaimRegistrationOrchestrator(
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val txTemplate: TransactionTemplate,
) {

    /**
     * 검증·조회·S3 업로드를 마친 입력으로 클레임을 등록하고 SF 송신을 트리거한다.
     *
     * @param channel 등록 경로 (web=WEB, mobile=CAP)
     * @param photos 업로드 완료된 사진 (defect/part 필수, receipt 선택)
     * @param onAfterInsert Tx 내부에서 INSERT 직후 실행할 후처리 (예: mobile draft 삭제). 트랜잭션에 포함된다.
     * @return 등록된 [Claim] (status=SF_PENDING). SF 전송 결과는 커밋 후 비동기로 전이된다.
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
    ): Claim {
        // 1. Transaction — DB INSERT (status=SF_PENDING) + 커밋 후 SF 송신 이벤트 발행.
        // publishEvent 를 트랜잭션 안에서 호출하지만, @TransactionalEventListener(AFTER_COMMIT) 이므로
        // 실제 SF 송신은 본 트랜잭션 커밋 이후에 일어난다 (claim/photo 영속화 보장).
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
            // 커밋 후 SF 송신 트리거 (AFTER_COMMIT + @Async). claimId 만 운반하고
            // SF 입력·이미지는 ClaimSfDispatchService 가 DB + S3 에서 복원한다.
            eventPublisher.publishEvent(ClaimRegisteredEvent(saved.id))
            saved
        }!!

        return savedClaim
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
