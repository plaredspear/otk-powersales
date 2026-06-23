package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.domain.activity.claim.exception.ClaimTypeHierarchyMismatchException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimDateException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimType1Exception
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimType2Exception
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateTypeException
import com.otoki.powersales.domain.activity.claim.exception.InvalidPurchaseMethodException
import com.otoki.powersales.domain.activity.claim.exception.InvalidRequestTypeException
import com.otoki.powersales.domain.activity.claim.exception.ReceiptRequiredException
import com.otoki.powersales.domain.activity.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.promotion.exception.AccountNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Web admin 클레임 등록 — dual-write (Spec #829).
 *
 * web 입력(AdminClaimCreateRequest) 파싱·검증 + 의존 entity 조회(employeeCode/externalKey 기반) 를 책임지고,
 * 등록 골격(Tx 분할 + SF 호출)은 [ClaimRegistrationCore] 에, SF 전송 로직은 [ClaimSfOutboundService] 에 위임한다.
 *
 * SF 호출 실패는 claim 을 SEND_FAILED 로 보존한다 — 사용자는 [AdminClaimResendService] 로 수동 재전송 가능.
 */
@Service
class AdminClaimCreateService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService,
    private val registrationCore: ClaimRegistrationCore,
) {

    // 등록 골격(ClaimRegistrationCore)이 txTemplate 으로 트랜잭션 경계를 직접 관리하므로 진입 시점엔
    // 트랜잭션이 없어야 한다 — NEVER 로 상위 readOnly 트랜잭션 상속을 능동 차단.
    @Transactional(propagation = Propagation.NEVER)
    fun createClaim(
        request: AdminClaimCreateRequest,
        claimPhoto: MultipartFile,
        partPhoto: MultipartFile,
        receiptPhoto: MultipartFile?,
    ): AdminClaimCreateResponse {
        // 1. 검증 + 정규화
        val parsed = parseRequest(request, receiptPhoto)

        // 2. 의존 entity 조회 (미존재 시 422)
        val employee = employeeRepository.findByEmployeeCode(parsed.employeeCode)
            .orElseThrow { ClaimInvalidParameterException("사원을 찾을 수 없습니다: ${parsed.employeeCode}") }
        val account = accountRepository.findByExternalKey(parsed.sapAccountCode)
            ?: throw AccountNotFoundException()
        val product = productRepository.findByProductCode(parsed.productCode)
            ?: throw ProductNotFoundException(parsed.productCode)

        // 3. S3 이미지 업로드 (트랜잭션 외부)
        val claimKey = fileStorageService.uploadClaimPhoto(claimPhoto, employee.id, 0L, UploadFileKbnTypes.CLAIM_DEFECT)
        val partKey = fileStorageService.uploadClaimPhoto(partPhoto, employee.id, 0L, UploadFileKbnTypes.CLAIM_PART)
        val receiptKey = receiptPhoto?.let {
            fileStorageService.uploadClaimPhoto(it, employee.id, 0L, UploadFileKbnTypes.CLAIM_RECEIPT)
        }

        // 4. 등록 골격 위임 (Tx1 INSERT → SF call → Tx2 status update)
        val result = registrationCore.register(
            employee = employee,
            account = account,
            product = product,
            parsed = parsed,
            channel = ClaimChannel.WEB,
            photos = ClaimRegistrationCore.ClaimPhotos(
                defectPhoto = claimPhoto,
                defectKey = claimKey,
                partPhoto = partPhoto,
                partKey = partKey,
                receiptPhoto = receiptPhoto,
                receiptKey = receiptKey,
            ),
        )

        val sfResult = result.sfResult
        return AdminClaimCreateResponse(
            claimId = result.claim.id,
            status = result.claim.status?.name ?: "",
            sfResultCode = sfResult.apiResponse?.resultCode,
            sfResultMsg = sfResult.apiResponse?.resultMsg ?: sfResult.errorSummary,
        )
    }

    /**
     * web 입력 검증 + 정규화 → 공용 [ClaimSfOutboundService.ParsedInput].
     * SF 전송 테스트 도구([AdminClaimRegistTestService])도 운영 경로와 동일 규칙 재사용을 위해 호출한다.
     */
    internal fun parseRequest(
        request: AdminClaimCreateRequest,
        receiptPhoto: MultipartFile?,
    ): ClaimSfOutboundService.ParsedInput {
        val dateType = parseDateType(request.dateType!!)
        val date = when (dateType) {
            ClaimDateType.EXPIRY_DATE -> parseDate(
                request.expirationDate
                    ?: throw ClaimInvalidParameterException("dateType=EXPIRY_DATE 시 expirationDate 필수"),
                "expirationDate",
            )
            ClaimDateType.MANUFACTURE_DATE -> parseDate(
                request.manufacturingDate
                    ?: throw ClaimInvalidParameterException("dateType=MANUFACTURE_DATE 시 manufacturingDate 필수"),
                "manufacturingDate",
            )
        }
        if (dateType == ClaimDateType.MANUFACTURE_DATE && date.isAfter(LocalDate.now())) {
            throw InvalidClaimDateException("제조일자를 다시한번 확인해주십시오.")
        }
        val claimDate = parseDate(request.claimDate!!, "claimDate")
        if (claimDate.isAfter(LocalDate.now())) {
            throw InvalidClaimDateException("발생일자를 다시한번 확인해주십시오.")
        }
        val claimType1 = ClaimType1.fromValueOrNull(request.claimType1)
            ?: throw InvalidClaimType1Exception()
        val claimType2 = ClaimType2.fromValueOrNull(request.claimType2)
            ?: throw InvalidClaimType2Exception()
        if (claimType2.parent != claimType1) {
            throw ClaimTypeHierarchyMismatchException()
        }
        val purchaseMethod = request.purchaseMethod?.takeIf { it.isNotBlank() }?.let {
            PurchaseMethod.fromSfValueOrNull(it) ?: throw InvalidPurchaseMethodException()
        }
        val amount = request.amount
        // 영수증 조건부 정책 (Q12): purchaseMethod ∈ {B, C} 면 receipt 필수.
        if (purchaseMethod == PurchaseMethod.PERSONAL_CARD || purchaseMethod == PurchaseMethod.CASH) {
            if (receiptPhoto == null) throw ReceiptRequiredException()
            if (amount == null || amount <= BigDecimal.ZERO) {
                throw ClaimInvalidParameterException("개인카드/현금 구매 시 구매 금액은 양수여야 합니다")
            }
        }
        val requestTypes = parseRequestTypes(request.requestType)
        return ClaimSfOutboundService.ParsedInput(
            sapAccountCode = request.sapAccountCode!!,
            productCode = request.productCode!!,
            employeeCode = request.employeeCode!!,
            dateType = dateType,
            date = date,
            claimDate = claimDate,
            claimType1 = claimType1,
            claimType2 = claimType2,
            quantity = request.quantity!!,
            description = request.description!!,
            purchaseMethod = purchaseMethod,
            amount = amount,
            requestTypes = requestTypes,
        )
    }

    private fun parseDateType(raw: String): ClaimDateType =
        try {
            ClaimDateType.valueOf(raw)
        } catch (e: IllegalArgumentException) {
            throw InvalidDateTypeException()
        }

    private fun parseDate(raw: String, fieldName: String): LocalDate =
        try {
            LocalDate.parse(raw)
        } catch (e: DateTimeException) {
            throw ClaimInvalidParameterException("$fieldName 형식이 올바르지 않습니다 (yyyy-MM-dd 필요)")
        }

    private fun parseRequestTypes(raw: String?): Set<RequestType> {
        if (raw.isNullOrBlank()) return emptySet()
        val tokens = raw.split(";", ",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.size > 4) throw RequestTypeMaxExceededException()
        return tokens.map {
            RequestType.fromDisplayNameOrNull(it) ?: throw InvalidRequestTypeException()
        }.toSet()
    }
}
