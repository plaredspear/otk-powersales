package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
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
import com.otoki.powersales.domain.activity.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.domain.activity.claim.repository.ClaimDraftRepository
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.exception.ProductNotFoundException
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileKbnTypes
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.promotion.exception.AccountNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val storageService: StorageService
) {

    /**
     * 클레임 등록 (UC-02/UC-10 모바일 REST).
     *
     * 레거시 ClaimTrigger 정합:
     *   - 제조일자 미래 차단
     *   - 요청사항 최대 4개
     *   - 접수사원·부서 자동 채움 (Employee → costCenter/orgName)
     *   - CC코드 자동 복사 (Account.branchCode → costCenterCode)
     *
     * @param userId UserPrincipal.userId — Employee.id 와 동일 (조회 정책: ClaimQueryService 와 정합)
     */
    @Transactional
    fun createClaim(
        userId: Long,
        request: ClaimCreateRequest,
        defectPhoto: MultipartFile,
        labelPhoto: MultipartFile,
        receiptPhoto: MultipartFile?
    ): ClaimCreateResponse {
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
            status = ClaimStatus.DRAFT,
            product = product,
            // 레거시 Trigger 정합: 접수사원·부서 자동 채움 (HR 코드 미러)
            // CC코드 자동 복사: Employee.costCenterCode 우선, 없으면 Account.branchCode
            costCenterCode = employee.costCenterCode ?: account.branchCode,
            division = employee.orgName
        )

        val savedClaim = claimRepository.save(claim)

        uploadPhoto(savedClaim, userId, defectPhoto, UploadFileKbnTypes.CLAIM_DEFECT)
        uploadPhoto(savedClaim, userId, labelPhoto, UploadFileKbnTypes.CLAIM_PART)
        if (receiptPhoto != null) {
            uploadPhoto(savedClaim, userId, receiptPhoto, UploadFileKbnTypes.CLAIM_RECEIPT)
        }

        // 레거시 정합: 정식 등록 성공 시 해당 사원의 임시저장 row 삭제.
        claimDraftRepository.findByEmployeeId(userId)?.let { claimDraftRepository.delete(it) }

        return ClaimCreateResponse.from(savedClaim)
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
            if (claim.claimType2.parent != t1) {
                throw ClaimTypeHierarchyMismatchException()
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

    private fun uploadPhoto(claim: Claim, userId: Long, file: MultipartFile, uploadKbn: String): UploadFile {
        val key = fileStorageService.uploadClaimPhoto(file, userId, claim.id, uploadKbn)
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
