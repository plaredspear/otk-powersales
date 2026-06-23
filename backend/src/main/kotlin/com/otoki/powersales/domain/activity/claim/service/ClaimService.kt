package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.exception.ClaimAccessDeniedException
import com.otoki.powersales.domain.activity.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotEditableException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.exception.ClaimPhotoNotFoundException
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
import com.otoki.powersales.domain.activity.claim.repository.ClaimDraftRepository
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.promotion.exception.AccountNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ClaimService(
    private val claimRepository: ClaimRepository,
    private val claimDraftRepository: ClaimDraftRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService,
    // SF outbound dual-write — 모바일 등록도 web admin 과 동일하게 SF /ClaimRegist 호출.
    private val sfClaimCreateService: AdminClaimCreateService,
    private val txTemplate: TransactionTemplate
) {

    /**
     * 클레임 등록 (UC-02/UC-10 모바일 REST) — SF dual-write.
     *
     * 레거시 ClaimTrigger / IF_REST_MOBILE_ClaimRegist 정합:
     *   - 제조일자 미래 차단
     *   - 요청사항 최대 4개
     *   - 접수사원·부서 자동 채움 (Employee → costCenter/orgName)
     *   - CC코드 자동 복사 (Account.branchCode → costCenterCode)
     *   - 등록 즉시 SF `/ClaimRegist` 호출 (channel=CAP — 레거시 모바일 정합)
     *
     * 처리 흐름 (web admin [AdminClaimCreateService] 와 동일 구조):
     *   1. 검증 + 의존 entity 조회
     *   2. S3 이미지 업로드 (트랜잭션 외부)
     *   3. [Transaction 1] claim + photo INSERT (status=SF_PENDING, channel=CAP)
     *   4. [SF call] SfOutboundClient.callApi("/ClaimRegist")
     *   5. [Transaction 2] status update (성공 → SENT, 실패 → SEND_FAILED)
     *
     * SF 호출 실패는 catch 하여 status=SEND_FAILED 로 응답한다 — HTTP 5xx 로 반환하지 않음.
     *
     * @param userId UserPrincipal.userId — Employee.id 와 동일 (조회 정책: ClaimQueryService 와 정합)
     */
    fun createClaim(
        userId: Long,
        request: ClaimCreateRequest,
        defectPhoto: MultipartFile,
        labelPhoto: MultipartFile,
        receiptPhoto: MultipartFile?
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

        // 영수증 조건부 필수 (Spec #829 / 레거시 write.jsp): 개인카드(B)·현금(C) 면 영수증 필수, 법인카드(A) 면제.
        if ((purchaseMethod == PurchaseMethod.PERSONAL_CARD || purchaseMethod == PurchaseMethod.CASH) &&
            receiptPhoto == null
        ) {
            throw ReceiptRequiredException()
        }

        // 2. S3 이미지 업로드 (트랜잭션 외부)
        val defectKey = fileStorageService.uploadClaimPhoto(defectPhoto, userId, 0L, UploadFileKbnTypes.CLAIM_DEFECT)
        val labelKey = fileStorageService.uploadClaimPhoto(labelPhoto, userId, 0L, UploadFileKbnTypes.CLAIM_PART)
        val receiptKey = receiptPhoto?.let {
            fileStorageService.uploadClaimPhoto(it, userId, 0L, UploadFileKbnTypes.CLAIM_RECEIPT)
        }

        // 3. Transaction 1 — DB INSERT (status=SF_PENDING, channel=CAP)
        val savedClaim = txTemplate.execute {
            val claim = Claim(
                employee = employee,
                account = account,
                dateType = dateType,
                date = date,
                claimType1 = claimType1,
                claimType2 = claimType2,
                defectDescription = request.defectDescription!!,
                defectQuantity = request.defectQuantity!!,
                purchaseAmount = request.purchaseAmount,
                purchaseMethodCode = purchaseMethod,
                requestTypeCode = requestTypes,
                status = ClaimStatus.SF_PENDING,
                channel = ClaimChannel.CAP,
                product = product,
                // 레거시 Trigger 정합: 접수사원·부서 자동 채움 (HR 코드 미러)
                // CC코드 자동 복사: Employee.costCenterCode 우선, 없으면 Account.branchCode
                costCenterCode = employee.costCenterCode ?: account.branchCode,
                division = employee.orgName
            )
            val saved = claimRepository.save(claim)
            savePhoto(saved, defectPhoto, defectKey, UploadFileKbnTypes.CLAIM_DEFECT)
            savePhoto(saved, labelPhoto, labelKey, UploadFileKbnTypes.CLAIM_PART)
            if (receiptPhoto != null && receiptKey != null) {
                savePhoto(saved, receiptPhoto, receiptKey, UploadFileKbnTypes.CLAIM_RECEIPT)
            }
            // 레거시 정합: 정식 등록 성공 시 해당 사원의 임시저장 row 삭제.
            claimDraftRepository.findByEmployeeId(userId)?.let { claimDraftRepository.delete(it) }
            saved
        }!!

        // 4. SF call (트랜잭션 외부) — channel=CAP (레거시 모바일 정합)
        val sfResult = sfClaimCreateService.pushToSf(
            employeeCode = employee.employeeCode
                ?: throw ClaimInvalidParameterException("사번 미보유 사원은 클레임을 전송할 수 없습니다"),
            sapAccountCode = account.externalKey
                ?: throw ClaimInvalidParameterException("거래처 SAP 코드가 없어 클레임을 전송할 수 없습니다"),
            productCode = request.productCode!!,
            parsed = AdminClaimCreateService.ParsedInput(
                sapAccountCode = account.externalKey!!,
                productCode = request.productCode!!,
                employeeCode = employee.employeeCode!!,
                dateType = dateType,
                date = date,
                // 레거시 모바일은 ClaimDate=todayReg(오늘) 전송 — 발생일자=등록일.
                claimDate = LocalDate.now(),
                claimType1 = claimType1,
                claimType2 = claimType2,
                quantity = request.defectQuantity!!,
                description = request.defectDescription!!,
                purchaseMethod = purchaseMethod,
                amount = request.purchaseAmount,
                requestTypes = requestTypes
            ),
            channel = ClaimChannel.CAP.name,
            claimPhoto = defectPhoto,
            claimKey = defectKey,
            partPhoto = labelPhoto,
            partKey = labelKey,
            receiptPhoto = receiptPhoto,
            receiptKey = receiptKey
        )

        // 5. Transaction 2 — status update
        return txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(savedClaim.id)
                ?: throw ClaimNotFoundException(savedClaim.id)
            sfClaimCreateService.applySfResultToClaim(claim, sfResult)
            ClaimCreateResponse.from(claim)
        }!!
    }

    /**
     * 클레임 수정 (UC-03).
     * 상태가 DRAFT(임시저장) 일 때만 허용. 권한: 작성자 본인만.
     */
    @Transactional
    fun updateClaim(
        userId: Long,
        claimId: Long,
        request: ClaimUpdateRequest
    ): ClaimCreateResponse {
        val claim = findEditableClaim(userId, claimId)

        request.accountId?.let {
            val account = accountRepository.findByIdOrNull(it)
                ?: throw AccountNotFoundException()
            claim.account = account
            // CC코드 자동 복사 — Account 변경 시 branchCode 로 재복사
            claim.costCenterCode = claim.employee?.costCenterCode ?: account.branchCode
        }
        request.productCode?.let {
            val product = productRepository.findByProductCode(it)
                ?: throw ProductNotFoundException(it)
            claim.product = product
        }
        request.dateType?.let { claim.dateType = parseDateType(it) }
        request.date?.let {
            val newDate = parseDate(it)
            validateClaimDate(newDate, claim.dateType)
            claim.date = newDate
        }
        request.claimType1?.let {
            val t1 = ClaimType1.fromValueOrNull(it) ?: throw InvalidClaimType1Exception()
            claim.claimType1 = t1
            claim.claimType2?.let { ct2 ->
                if (ct2.parent != t1) {
                    throw ClaimTypeHierarchyMismatchException()
                }
            }
        }
        request.claimType2?.let {
            val t2 = ClaimType2.fromValueOrNull(it) ?: throw InvalidClaimType2Exception()
            if (t2.parent != claim.claimType1) {
                throw ClaimTypeHierarchyMismatchException()
            }
            claim.claimType2 = t2
        }
        request.defectDescription?.let { claim.defectDescription = it }
        request.defectQuantity?.let { claim.defectQuantity = it }
        request.purchaseAmount?.let { claim.purchaseAmount = it }
        request.purchaseMethodCode?.let {
            val pm = PurchaseMethod.fromSfValueOrNull(it) ?: throw InvalidPurchaseMethodException()
            claim.purchaseMethodCode = pm
        }
        request.requestTypeCode?.let {
            val rts = resolveRequestTypes(it)
            claim.requestTypeCode = rts
        }

        return ClaimCreateResponse.from(claim)
    }

    /**
     * 클레임 삭제 (UC-11).
     * 상태가 DRAFT 일 때만 허용. 첨부 사진 cascade 삭제 + S3 파일 일괄 삭제.
     */
    @Transactional
    fun deleteClaim(userId: Long, claimId: Long) {
        val claim = findEditableClaim(userId, claimId)

        val photos = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.CLAIM, claimId)
        photos.forEach { photo ->
            photo.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
            photo.isDeleted = true
        }

        claimRepository.delete(claim)
    }

    /**
     * 클레임 사진 삭제 (UC-06).
     * 상태가 DRAFT 일 때만 허용. S3 파일 삭제 + UploadFile soft-delete.
     */
    @Transactional
    fun deletePhoto(userId: Long, claimId: Long, photoId: Long) {
        findEditableClaim(userId, claimId)

        val photo = uploadFileRepository
            .findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.CLAIM, claimId)
            ?: throw ClaimPhotoNotFoundException(photoId)

        photo.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
        photo.isDeleted = true
    }

    // ───── private helpers ─────

    private fun findEditableClaim(userId: Long, claimId: Long): Claim {
        val claim = claimRepository.findByIdOrNull(claimId)
            ?: throw ClaimNotFoundException(claimId)
        if (claim.employee?.id != userId) {
            throw ClaimAccessDeniedException()
        }
        if (claim.status != ClaimStatus.DRAFT) {
            throw ClaimNotEditableException()
        }
        return claim
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
     * "제조일자를 다시한번 확인해주십시오."
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

    /**
     * 이미 S3 업로드된 key 로 UploadFile row 만 저장 (업로드는 트랜잭션 외부에서 선행).
     */
    private fun savePhoto(claim: Claim, file: MultipartFile, key: String, uploadKbn: String): UploadFile {
        val uploadFile = UploadFile(
            name = file.originalFilename,
            uniqueKey = key,
            fileSize = file.size.toString(),
            parentType = UploadFileParentTypes.CLAIM,
            parentId = claim.id,
            uploadKbn = uploadKbn,
            isDeleted = false
        )
        return uploadFileRepository.save(uploadFile)
    }
}

/**
 * UC-03 클레임 수정 요청. 부분 업데이트 — 변경 필드만 non-null.
 */
data class ClaimUpdateRequest(
    val accountId: Long? = null,
    val productCode: String? = null,
    val dateType: String? = null,
    val date: String? = null,
    val claimType1: String? = null,
    val claimType2: String? = null,
    val defectDescription: String? = null,
    val defectQuantity: BigDecimal? = null,
    val purchaseAmount: BigDecimal? = null,
    val purchaseMethodCode: String? = null,
    val requestTypeCode: String? = null
)
