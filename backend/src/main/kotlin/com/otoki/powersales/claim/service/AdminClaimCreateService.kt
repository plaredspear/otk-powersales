package com.otoki.powersales.claim.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.claim.dto.response.AdminClaimCreateResponse
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.claim.enums.ClaimChannel
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.exception.ClaimTypeHierarchyMismatchException
import com.otoki.powersales.claim.exception.InvalidClaimDateException
import com.otoki.powersales.claim.exception.InvalidClaimType1Exception
import com.otoki.powersales.claim.exception.InvalidClaimType2Exception
import com.otoki.powersales.claim.exception.InvalidDateTypeException
import com.otoki.powersales.claim.exception.InvalidPurchaseMethodException
import com.otoki.powersales.claim.exception.InvalidRequestTypeException
import com.otoki.powersales.claim.exception.ReceiptRequiredException
import com.otoki.powersales.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.exception.ProductNotFoundException
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileKbnTypes
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.promotion.exception.AccountNotFoundException
import com.otoki.powersales.sf.outbound.SfApiResponse
import com.otoki.powersales.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Web admin 클레임 등록 — dual-write (Spec #829).
 *
 * 처리 흐름:
 *  1. 입력 검증 (필수 / 영수증 조건부 / 미래일자 / requestType max 4)
 *  2. Employee / Account / Product 조회 (미존재 시 422)
 *  3. S3 이미지 업로드 (트랜잭션 외부)
 *  4. [Transaction 1] backend.claim + claim_photo INSERT (status=SF_PENDING, channel=WEB)
 *  5. [SF call] apiMap 빌드 + SfOutboundClient.callApi("/ClaimRegist", apiMap)
 *  6. [Transaction 2] status update (성공 → SENT + sent_at, 실패 → SEND_FAILED + send_fail_message)
 *
 * SF 호출 실패는 catch 하여 status=SEND_FAILED 로 응답한다 — HTTP 5xx 로 반환하지 않음.
 * 사용자는 admin 화면에서 [AdminClaimResendService] 로 수동 재전송 가능.
 */
@Service
class AdminClaimCreateService(
    private val claimRepository: ClaimRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService,
    private val sfOutboundClient: SfOutboundClient,
    private val txTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun createClaim(
        request: AdminClaimCreateRequest,
        claimPhoto: MultipartFile,
        partPhoto: MultipartFile,
        receiptPhoto: MultipartFile?,
    ): AdminClaimCreateResponse {
        // 1. 검증 + 정규화
        val parsed = ParsedInput.from(request, receiptPhoto)

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

        // 4. Transaction 1 — DB INSERT (status=SF_PENDING)
        val savedClaim = txTemplate.execute {
            val claim = Claim(
                employee = employee,
                account = account,
                accountName = account.name,
                productCode = product.productCode,
                productName = product.name,
                dateType = parsed.dateType,
                date = parsed.date,
                claimType1 = parsed.claimType1,
                claimType2 = parsed.claimType2,
                defectDescription = parsed.description,
                defectQuantity = parsed.quantity,
                purchaseAmount = parsed.amount,
                purchaseMethodCode = parsed.purchaseMethod,
                purchaseMethodName = parsed.purchaseMethod?.displayName,
                requestTypeCode = parsed.requestTypes,
                requestTypeName = parsed.requestTypes.joinToString(";") { it.displayName }.ifBlank { null },
                status = ClaimStatus.SF_PENDING,
                channel = ClaimChannel.WEB,
                product = product,
                costCenterCode = employee.costCenterCode ?: account.branchCode,
                division = employee.orgName,
            )
            val saved = claimRepository.save(claim)
            val photos = mutableListOf<UploadFile>().apply {
                add(buildPhoto(saved, claimPhoto, claimKey, UploadFileKbnTypes.CLAIM_DEFECT))
                add(buildPhoto(saved, partPhoto, partKey, UploadFileKbnTypes.CLAIM_PART))
                if (receiptPhoto != null && receiptKey != null) {
                    add(buildPhoto(saved, receiptPhoto, receiptKey, UploadFileKbnTypes.CLAIM_RECEIPT))
                }
            }
            uploadFileRepository.saveAll(photos)
            saved
        }!!

        // 5. SF call (트랜잭션 외부)
        val sfResult = pushToSf(
            employeeCode = parsed.employeeCode,
            sapAccountCode = parsed.sapAccountCode,
            productCode = parsed.productCode,
            parsed = parsed,
            claimPhoto = claimPhoto,
            claimKey = claimKey,
            partPhoto = partPhoto,
            partKey = partKey,
            receiptPhoto = receiptPhoto,
            receiptKey = receiptKey,
        )

        // 6. Transaction 2 — status update
        return txTemplate.execute {
            val claim = claimRepository.findByIdOrNull(savedClaim.id)
                ?: throw ClaimNotFoundException(savedClaim.id)
            applySfResultToClaim(claim, sfResult)
            AdminClaimCreateResponse(
                claimId = claim.id,
                status = claim.status.name,
                sfResultCode = sfResult.apiResponse?.resultCode,
                sfResultMsg = sfResult.apiResponse?.resultMsg ?: sfResult.errorSummary,
            )
        }!!
    }

    /**
     * SF push — 실패해도 예외를 throw 하지 않고 [SfPushResult] 로 반환.
     * caller (createClaim / resend) 가 status update 분기에 사용.
     */
    internal fun pushToSf(
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        claimPhoto: MultipartFile,
        claimKey: String,
        partPhoto: MultipartFile,
        partKey: String,
        receiptPhoto: MultipartFile?,
        receiptKey: String?,
    ): SfPushResult {
        val apiMap = buildApiMap(
            employeeCode = employeeCode,
            sapAccountCode = sapAccountCode,
            productCode = productCode,
            parsed = parsed,
            claimPhoto = claimPhoto,
            partPhoto = partPhoto,
            receiptPhoto = receiptPhoto,
        )
        return invokeSf(apiMap)
    }

    /**
     * 재전송 흐름 — S3 에 이미 업로드된 이미지를 다시 읽어 Base64 인코딩.
     * StorageService.download 로 bytes 회수.
     */
    internal fun pushToSfFromStored(
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        claimKey: String,
        partKey: String,
        receiptKey: String?,
        claimPhotoFilename: String,
        claimPhotoContentType: String,
        partPhotoFilename: String,
        partPhotoContentType: String,
        receiptPhotoFilename: String?,
        receiptPhotoContentType: String?,
    ): SfPushResult {
        val claimBytes = storageService.download(claimKey)
        val partBytes = storageService.download(partKey)
        val receiptBytes = receiptKey?.let { storageService.download(it) }
        val apiMap = buildApiMapFromBytes(
            employeeCode = employeeCode,
            sapAccountCode = sapAccountCode,
            productCode = productCode,
            parsed = parsed,
            claimBytes = claimBytes,
            claimFilename = claimPhotoFilename,
            claimContentType = claimPhotoContentType,
            partBytes = partBytes,
            partFilename = partPhotoFilename,
            partContentType = partPhotoContentType,
            receiptBytes = receiptBytes,
            receiptFilename = receiptPhotoFilename,
            receiptContentType = receiptPhotoContentType,
        )
        return invokeSf(apiMap)
    }

    private fun invokeSf(apiMap: Map<String, Any?>): SfPushResult {
        return try {
            val response = sfOutboundClient.callApi("/ClaimRegist", apiMap)
            SfPushResult(success = response.isSuccess(), apiResponse = response, errorSummary = null)
        } catch (e: SfOAuthFailedException) {
            log.warn("[admin-claim-create] SF OAuth 실패: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[admin-claim-create] SF 호출 예외: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: e.javaClass.simpleName)
        }
    }

    internal fun applySfResultToClaim(claim: Claim, result: SfPushResult) {
        claim.sendAttemptCount = claim.sendAttemptCount + 1
        if (result.success) {
            claim.status = ClaimStatus.SENT
            claim.sentAt = LocalDateTime.now()
            claim.sendFailMessage = null
        } else {
            claim.status = ClaimStatus.SEND_FAILED
            claim.sendFailMessage = (result.apiResponse?.resultMsg ?: result.errorSummary)?.take(1000)
        }
    }

    private fun buildApiMap(
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        claimPhoto: MultipartFile,
        partPhoto: MultipartFile,
        receiptPhoto: MultipartFile?,
    ): Map<String, Any?> = buildApiMapFromBytes(
        employeeCode = employeeCode,
        sapAccountCode = sapAccountCode,
        productCode = productCode,
        parsed = parsed,
        claimBytes = claimPhoto.bytes,
        claimFilename = claimPhoto.originalFilename ?: "claim",
        claimContentType = claimPhoto.contentType ?: "image/jpeg",
        partBytes = partPhoto.bytes,
        partFilename = partPhoto.originalFilename ?: "part",
        partContentType = partPhoto.contentType ?: "image/jpeg",
        receiptBytes = receiptPhoto?.bytes,
        receiptFilename = receiptPhoto?.originalFilename,
        receiptContentType = receiptPhoto?.contentType,
    )

    private fun buildApiMapFromBytes(
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        claimBytes: ByteArray,
        claimFilename: String,
        claimContentType: String,
        partBytes: ByteArray,
        partFilename: String,
        partContentType: String,
        receiptBytes: ByteArray?,
        receiptFilename: String?,
        receiptContentType: String?,
    ): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        map["SAPAccountCode"] = sapAccountCode
        map["ProductCode"] = productCode
        map["ExpirationDate"] = if (parsed.dateType == ClaimDateType.EXPIRY_DATE) parsed.date.toSfDate() else ""
        map["ManufacturingDate"] = if (parsed.dateType == ClaimDateType.MANUFACTURE_DATE) parsed.date.toSfDate() else ""
        map["ClaimType1"] = parsed.claimType1.value
        map["ClaimType2"] = parsed.claimType2.value
        map["ClaimDate"] = parsed.claimDate.toSfDate()
        map["Quantity"] = parsed.quantity.toPlainString()
        map["PurchaseMethod"] = parsed.purchaseMethod?.sfValue ?: ""
        map["Amount"] = parsed.amount?.toPlainString() ?: ""
        map["RequestType"] = parsed.requestTypes.joinToString(";") { it.displayName }
        map["Description"] = parsed.description
        map["Channel"] = "WEB"
        map["EmployeeCode"] = employeeCode

        map["ClaimImageBuffer"] = Base64.getEncoder().encodeToString(claimBytes)
        map["ClaimImageFileName"] = splitFilename(claimFilename).first
        map["ClaimImageFileExtension"] = splitFilename(claimFilename).second.ifBlank { extFromContentType(claimContentType) }

        map["PartImageBuffer"] = Base64.getEncoder().encodeToString(partBytes)
        map["PartImageFileName"] = splitFilename(partFilename).first
        map["PartImageFileExtension"] = splitFilename(partFilename).second.ifBlank { extFromContentType(partContentType) }

        map["ReceiptImageBuffer"] = receiptBytes?.let { Base64.getEncoder().encodeToString(it) } ?: ""
        map["ReceiptImageFileName"] = receiptFilename?.let { splitFilename(it).first } ?: ""
        map["ReceiptImageFileExtension"] = receiptFilename?.let { splitFilename(it).second }
            ?: receiptContentType?.let { extFromContentType(it) }
            ?: ""

        return map
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

    private fun splitFilename(filename: String): Pair<String, String> {
        val dot = filename.lastIndexOf('.')
        return if (dot <= 0 || dot == filename.length - 1) {
            filename to ""
        } else {
            filename.substring(0, dot) to filename.substring(dot + 1)
        }
    }

    private fun extFromContentType(contentType: String): String = when (contentType.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        else -> ""
    }

    private fun LocalDate.toSfDate(): String = format(SF_DATE_FORMATTER)

    /**
     * 검증 + 정규화 결과 — service 내부에서 공유되는 입력 구조.
     */
    internal data class ParsedInput(
        val sapAccountCode: String,
        val productCode: String,
        val employeeCode: String,
        val dateType: ClaimDateType,
        val date: LocalDate,
        val claimDate: LocalDate,
        val claimType1: ClaimType1,
        val claimType2: ClaimType2,
        val quantity: BigDecimal,
        val description: String,
        val purchaseMethod: PurchaseMethod?,
        val amount: BigDecimal?,
        val requestTypes: Set<RequestType>,
    ) {
        companion object {
            fun from(request: AdminClaimCreateRequest, receiptPhoto: MultipartFile?): ParsedInput {
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
                return ParsedInput(
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
    }

    /** SF push 결과 — 성공/실패 + 응답 또는 오류 요약. */
    internal data class SfPushResult(
        val success: Boolean,
        val apiResponse: SfApiResponse?,
        val errorSummary: String?,
    )

    companion object {
        private val SF_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
