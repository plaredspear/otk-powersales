package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.domain.activity.claim.exception.ClaimTypeHierarchyMismatchException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimDateException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimType1Exception
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimType2Exception
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateFormatException
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateTypeException
import com.otoki.powersales.domain.activity.claim.exception.InvalidPurchaseMethodException
import com.otoki.powersales.domain.activity.claim.exception.InvalidRequestTypeException
import com.otoki.powersales.domain.activity.claim.exception.ReceiptRequiredException
import com.otoki.powersales.domain.activity.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import com.otoki.powersales.domain.activity.claim.repository.ClaimDraftRepository
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.promotion.exception.AccountNotFoundException
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

/**
 * 모바일 클레임 등록 (UC-02/UC-10) — SF dual-write (channel=CAP).
 *
 * 모바일 입력 파싱 + 의존 entity 조회(userId/accountId/productCode 기반) + draft 삭제 정책을 책임지고,
 * 트랜잭션 안에서 claim + photo 를 INSERT(sfSendStatus=PENDING)한 뒤 [ClaimRegisteredEvent] 를 발행한다.
 * SF `/ClaimRegist` 송신은 커밋 후 [ClaimSfPushDispatcher] 가 비동기로 수행하므로 본 service 는 SF I/O 무관.
 *
 * 레거시 ClaimTrigger / IF_REST_MOBILE_ClaimRegist 정합:
 *   - 제조일자 미래 차단
 *   - 요청사항 최대 4개
 *   - CC코드(cost_center_code)는 거래처(Account) BranchCode 로 자동 채움 (SF ClaimRegist.cls:90 정합) — [Claim.forRegistration] 처리
 *   - division 은 등록 시 공란 (SF 모바일 등록 정합) — [Claim.forRegistration] 처리
 *   - 정식 등록 성공 시 해당 사원의 임시저장 row 삭제
 *
 * 트랜잭션 경계를 [txTemplate] 으로 직접 관리하므로 진입 시점엔 트랜잭션이 없어야 한다 — 클래스 레벨
 * @Transactional 을 두지 않고, 진입 메서드는 NEVER 로 상위 readOnly 상속을 능동 차단한다.
 */
@Service
class MobileClaimService(
    private val claimDraftRepository: ClaimDraftRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService,
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val txTemplate: TransactionTemplate,
) {

    /**
     * @param userId UserPrincipal.userId — Employee.id 와 동일 (조회 정책: ClaimQueryService 와 정합)
     */
    // txTemplate 으로 트랜잭션 경계를 직접 관리하므로 진입 시점엔 트랜잭션이 없어야 한다 —
    // NEVER 로 상위 readOnly 트랜잭션 상속을 능동 차단.
    @Transactional(propagation = Propagation.NEVER)
    fun createClaim(
        userId: Long,
        request: ClaimCreateRequest,
        defectPhoto: MultipartFile,
        labelPhoto: MultipartFile,
        receiptPhoto: MultipartFile?,
    ): ClaimCreateResponse {
        // 1. 검증 + 의존 entity 조회
        val employee = employeeRepository.findByIdOrNull(userId)
            ?: throw ClaimInvalidParameterException("사원을 찾을 수 없습니다")
        val account = accountRepository.findByIdOrNull(request.accountId!!)
            ?: throw AccountNotFoundException()
        val product = productRepository.findByProductCode(request.productCode!!)
            ?: throw ProductNotFoundException(request.productCode)

        val dateType = parseDateType(request.dateType!!)
        val date = parseDate(request.date!!)
        validateClaimDate(date, dateType)

        val claimType1 = ClaimType1.fromValueOrNull(request.claimType1)
            ?: throw InvalidClaimType1Exception()
        val claimType2 = ClaimType2.fromValueOrNull(request.claimType2)
            ?: throw InvalidClaimType2Exception()
        if (claimType2.parent != claimType1) {
            throw ClaimTypeHierarchyMismatchException()
        }

        val purchaseMethod = resolvePurchaseMethod(request.purchaseMethodCode)
        val requestTypes = resolveRequestTypes(request.requestTypeCode)

        // 영수증 조건부 필수: 개인카드(B)·현금(C) 면 영수증 필수, 법인카드(A) 면제.
        if ((purchaseMethod == PurchaseMethod.PERSONAL_CARD || purchaseMethod == PurchaseMethod.CASH) &&
            receiptPhoto == null
        ) {
            throw ReceiptRequiredException()
        }

        val employeeCode = employee.employeeCode
            ?: throw ClaimInvalidParameterException("사번 미보유 사원은 클레임을 전송할 수 없습니다")
        val sapAccountCode = account.externalKey
            ?: throw ClaimInvalidParameterException("거래처 SAP 코드가 없어 클레임을 전송할 수 없습니다")

        // 2. S3 이미지 업로드 (트랜잭션 외부)
        val defectKey = fileStorageService.uploadClaimPhoto(defectPhoto, userId, 0L, UploadFileKbnTypes.CLAIM_DEFECT)
        val labelKey = fileStorageService.uploadClaimPhoto(labelPhoto, userId, 0L, UploadFileKbnTypes.CLAIM_PART)
        val receiptKey = receiptPhoto?.let {
            fileStorageService.uploadClaimPhoto(it, userId, 0L, UploadFileKbnTypes.CLAIM_RECEIPT)
        }

        // 3. Tx INSERT (status=DRAFT, sfSendStatus=PENDING) + draft 삭제 + 커밋 후 SF 송신 이벤트 발행.
        // SF 송신은 커밋 후 비동기(ClaimSfPushDispatcher) — 본 응답은 claim 정보만 반환한다.
        val claim = txTemplate.execute {
            val saved = claimRepository.save(
                Claim.forRegistration(
                    employee = employee,
                    account = account,
                    product = product,
                    channel = ClaimChannel.CAP,
                    dateType = dateType,
                    date = date,
                    claimType1 = claimType1,
                    claimType2 = claimType2,
                    quantity = request.defectQuantity!!,
                    description = request.defectDescription!!,
                    purchaseMethod = purchaseMethod,
                    amount = request.purchaseAmount,
                    requestTypes = requestTypes,
                ),
            )
            uploadFileRepository.saveAll(
                buildClaimPhotoRows(saved, defectPhoto, defectKey, labelPhoto, labelKey, receiptPhoto, receiptKey),
            )
            // 레거시 정합: 정식 등록 성공 시 해당 사원의 임시저장 row 삭제.
            claimDraftRepository.findByEmployeeId(userId)?.let { claimDraftRepository.delete(it) }
            // 커밋 후 SF 송신 트리거 — claimId 만 운반, SF 입력·이미지는 ClaimSfDispatchService 가 DB+S3 에서 복원.
            eventPublisher.publishEvent(ClaimRegisteredEvent(saved.id))
            saved
        }!!

        return ClaimCreateResponse.from(claim)
    }

    private fun parseDateType(raw: String): ClaimDateType =
        try {
            ClaimDateType.valueOf(raw)
        } catch (e: IllegalArgumentException) {
            throw InvalidDateTypeException()
        }

    private fun parseDate(raw: String): LocalDate =
        try {
            LocalDate.parse(raw)
        } catch (e: Exception) {
            throw InvalidDateFormatException()
        }

    /**
     * 레거시 ClaimTrigger 정합: 제조일자 미래 차단.
     */
    private fun validateClaimDate(date: LocalDate, dateType: ClaimDateType?) {
        if (dateType == ClaimDateType.MANUFACTURE_DATE && date.isAfter(LocalDate.now())) {
            throw InvalidClaimDateException("제조일자를 다시한번 확인해주십시오.")
        }
    }

    private fun resolvePurchaseMethod(sfValue: String?): PurchaseMethod? {
        if (sfValue.isNullOrBlank()) return null
        return PurchaseMethod.fromSfValueOrNull(sfValue) ?: throw InvalidPurchaseMethodException()
    }

    /**
     * 레거시 Validation Rule (RequestTypeRule) 정합: 최대 4개.
     */
    private fun resolveRequestTypes(raw: String?): Set<RequestType> {
        if (raw.isNullOrBlank()) return emptySet()
        val tokens = raw.split(";", ",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.size > 4) {
            throw RequestTypeMaxExceededException()
        }
        return tokens.map {
            RequestType.fromDisplayNameOrNull(it) ?: throw InvalidRequestTypeException()
        }.toSet()
    }
}
