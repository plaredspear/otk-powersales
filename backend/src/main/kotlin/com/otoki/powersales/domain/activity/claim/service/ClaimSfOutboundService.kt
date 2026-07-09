package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * 클레임 SF outbound 허브 — `/ClaimRegist` dual-write 의 공용 전송 로직.
 *
 * 등록 (web [AdminClaimCreateService] · mobile [MobileClaimService]) 과 재전송
 * ([AdminClaimResendService]) 이 공통으로 의존한다. 진입점별 입력 파싱/조회 정책은 각
 * 진입점이 책임지고, 본 service 는 "검증 완료된 [ParsedInput] → SF 호출 → 결과 적용" 만 담당한다.
 *
 * SF 호출 실패는 throw 하지 않고 [SfPushResult] 로 반환한다 — caller 가 status 전이에 사용.
 */
@Service
class ClaimSfOutboundService(
    private val storageService: StorageService,
    private val sfOutboundClient: SfOutboundClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 신규 등록 push — MultipartFile bytes 를 직접 Base64 인코딩.
     *
     * @param pwrskey 등록된 claim 의 PowerSales primary key (`claim_id`). SF 역연결용. [buildApiMapFromBytes] 참조.
     */
    fun pushToSf(
        pwrskey: Long,
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        channel: String,
        claimPhoto: MultipartFile,
        claimKey: String,
        partPhoto: MultipartFile,
        partKey: String,
        receiptPhoto: MultipartFile?,
        receiptKey: String?,
    ): SfPushResult {
        val apiMap = buildApiMap(
            pwrskey = pwrskey,
            employeeCode = employeeCode,
            sapAccountCode = sapAccountCode,
            productCode = productCode,
            parsed = parsed,
            channel = channel,
            claimPhoto = claimPhoto,
            partPhoto = partPhoto,
            receiptPhoto = receiptPhoto,
        )
        return invokeSf(apiMap)
    }

    /**
     * 재전송 push — S3 에 이미 업로드된 이미지를 다시 읽어 Base64 인코딩.
     *
     * @param pwrskey 재전송 대상 claim 의 PowerSales primary key (`claim_id`). SF 역연결용. [buildApiMapFromBytes] 참조.
     */
    fun pushToSfFromStored(
        pwrskey: Long,
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        channel: String,
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
        val claimBytes = storageService.downloadPrivate(claimKey)
        val partBytes = storageService.downloadPrivate(partKey)
        val receiptBytes = receiptKey?.let { storageService.downloadPrivate(it) }
        val apiMap = buildApiMapFromBytes(
            pwrskey = pwrskey,
            employeeCode = employeeCode,
            sapAccountCode = sapAccountCode,
            productCode = productCode,
            parsed = parsed,
            channel = channel,
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

    fun invokeSf(apiMap: Map<String, Any?>): SfPushResult {
        return try {
            val response = sfOutboundClient.callApi("/ClaimRegist", apiMap)
            SfPushResult(success = response.isSuccess(), apiResponse = response, errorSummary = null)
        } catch (e: SfOAuthFailedException) {
            log.warn("[claim-sf-outbound] SF OAuth 실패: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[claim-sf-outbound] SF 호출 예외: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: e.javaClass.simpleName)
        }
    }

    fun applySfResultToClaim(claim: Claim, result: SfPushResult) {
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
        pwrskey: Long,
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        channel: String,
        claimPhoto: MultipartFile,
        partPhoto: MultipartFile,
        receiptPhoto: MultipartFile?,
    ): Map<String, Any?> = buildApiMapFromBytes(
        pwrskey = pwrskey,
        employeeCode = employeeCode,
        sapAccountCode = sapAccountCode,
        productCode = productCode,
        parsed = parsed,
        channel = channel,
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

    /**
     * @param pwrskey 해당 클레임 레코드의 PowerSales primary key (`claim_id`). SF 가 등록된 SF 레코드와
     * PowerSales row 를 역연결(back-link)하도록 전송한다.
     * ⚠️ 선행조건: 레거시 SF Apex `IF_REST_MOBILE_ClaimRegist` 는 `JSON.deserializeStrict` 로 본문을 파싱하므로
     * SF `Input` 클래스에 `public String pwrskey;` 필드가 추가 배포돼 있어야 한다. 미배포 상태로 전송하면 strict
     * 파싱이 실패해 전 건 SEND_FAILED 가 된다 (제안 `ProposalRegist` pwrskey 추가와 동일 패턴/제약).
     */
    fun buildApiMapFromBytes(
        pwrskey: Long,
        employeeCode: String,
        sapAccountCode: String,
        productCode: String,
        parsed: ParsedInput,
        channel: String,
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
        map["pwrskey"] = pwrskey.toString()
        map["SAPAccountCode"] = sapAccountCode
        map["ProductCode"] = productCode
        map["ExpirationDate"] = if (parsed.dateType == ClaimDateType.EXPIRY_DATE) parsed.date.toSfDate() else ""
        map["ManufacturingDate"] = if (parsed.dateType == ClaimDateType.MANUFACTURE_DATE) parsed.date.toSfDate() else ""
        map["ClaimType1"] = parsed.claimType1.value
        map["ClaimType2"] = parsed.claimType2.value
        map["ClaimDate"] = parsed.claimDate.toSfDate()
        map["Quantity"] = parsed.quantity.toPlainString()
        map["PurchaseMethod"] = parsed.purchaseMethod?.sfValue ?: ""
        // SF Apex `IF_REST_MOBILE_ClaimRegist` 는 `if(Amount != null) Decimal.valueOf(Amount)` 로 파싱한다.
        // 빈 문자열을 보내면 `Decimal.valueOf("")` 가 Exception 을 던지므로, 미입력 시 JSON null 로 전송한다.
        map["Amount"] = parsed.amount?.toPlainString()
        map["RequestType"] = parsed.requestTypes.joinToString(";") { it.displayName }
        map["Description"] = parsed.description
        // SF Apex `IF_REST_MOBILE_ClaimRegist` 는 Input.Channel 을 무시하고 Channel='CAP' 로 하드코딩하나,
        // 신규는 backend.claim.channel 추적값과 정합을 위해 명시 전송한다 (web=WEB, mobile=CAP).
        map["Channel"] = channel
        map["EmployeeCode"] = employeeCode

        map["ClaimImageBuffer"] = Base64.getEncoder().encodeToString(claimBytes)
        map["ClaimImageFileName"] = splitFilename(claimFilename).first
        map["ClaimImageFileExtension"] = resolveImageExtension(claimFilename, claimContentType)

        map["PartImageBuffer"] = Base64.getEncoder().encodeToString(partBytes)
        map["PartImageFileName"] = splitFilename(partFilename).first
        map["PartImageFileExtension"] = resolveImageExtension(partFilename, partContentType)

        map["ReceiptImageBuffer"] = receiptBytes?.let { Base64.getEncoder().encodeToString(it) } ?: ""
        map["ReceiptImageFileName"] = receiptFilename?.let { splitFilename(it).first } ?: ""
        map["ReceiptImageFileExtension"] = receiptFilename?.let { resolveImageExtension(it, receiptContentType) } ?: ""

        return map
    }

    /**
     * SF `ContentVersion` 확장자 결정 — SF `FileExtensionGuard` 의 허용 확장자 정합.
     *
     * SF Apex `IF_REST_MOBILE_ClaimRegist` 는 `PathOnClient = 파일명 + '.' + 확장자` 로 조립하고,
     * `ContentVersionTrigger` → `FileExtensionGuard` 가 이 확장자를 허용목록(File_Extension_Policy__mdt)
     * 과 대조해 거부하면 `FIELD_CUSTOM_VALIDATION_EXCEPTION` 으로 전 건이 SEND_FAILED 가 된다.
     *
     * iOS(Simulator 포함) image_picker 임시 파일은 확장자가 없거나(`...tmp`) 비이미지 확장자로 넘어와
     * 파일명만 믿으면 빈값/`tmp` 가 그대로 전송돼 차단된다. 클레임 첨부는 항상 이미지이므로:
     *   1) 파일명 확장자가 허용 이미지 확장자면 그대로 사용,
     *   2) 아니면 contentType 기반 확장자로 대체,
     *   3) 그래도 미확정이면 `jpg` 로 안전 fallback (backend 는 image/jpeg 로 인코딩·전송한다).
     */
    private fun resolveImageExtension(filename: String, contentType: String?): String {
        val fromName = splitFilename(filename).second.lowercase()
        if (fromName in ALLOWED_IMAGE_EXTENSIONS) return fromName
        val fromContentType = contentType?.let { extFromContentType(it) }.orEmpty()
        if (fromContentType.isNotBlank()) return fromContentType
        return DEFAULT_IMAGE_EXTENSION
    }

    private fun splitFilename(filename: String): Pair<String, String> {
        val dot = filename.lastIndexOf('.')
        return if (dot <= 0 || dot == filename.length - 1) {
            filename to ""
        } else {
            filename.substring(0, dot) to filename.substring(dot + 1)
        }
    }

    private fun extFromContentType(contentType: String): String = when (contentType.lowercase().substringBefore(';').trim()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/heic", "image/heif" -> "heic"
        "image/tiff" -> "tif"
        else -> ""
    }

    private fun LocalDate.toSfDate(): String = format(SF_DATE_FORMATTER)

    /**
     * 검증 + 정규화 완료된 클레임 입력 — 진입점별 파싱 결과의 공용 구조.
     * 파싱/검증 책임은 각 진입점(web=[AdminClaimCreateService], mobile=[MobileClaimService])에 있다.
     */
    data class ParsedInput(
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
    )

    /** SF push 결과 — 성공/실패 + 응답 또는 오류 요약. */
    data class SfPushResult(
        val success: Boolean,
        val apiResponse: SfApiResponse?,
        val errorSummary: String?,
    )

    companion object {
        // SF Apex `IF_REST_MOBILE_ClaimRegist` 는 날짜 3종 (ExpirationDate/ManufacturingDate/ClaimDate) 을
        // `Date.valueOf(String)` 으로 파싱한다 — Apex `Date.valueOf` 는 'YYYY-MM-DD' (ISO) 만 받으므로
        // 'yyyyMMdd' 를 보내면 Exception → 전송 실패한다. 레거시 Heroku 도 ISO 포맷으로 전송했다.
        private val SF_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        // SF `FileExtensionGuard` 허용 확장자(File_Extension_Policy__mdt) 중 클레임 첨부에 해당하는 이미지
        // 확장자. 파일명에서 뽑은 확장자가 이 집합이면 그대로 사용, 아니면 contentType 기반으로 정규화한다.
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "heic", "jfif", "tif")

        // 확장자를 파일명·contentType 어느 쪽으로도 확정하지 못했을 때의 안전 fallback.
        // backend 는 이미지를 image/jpeg 로 인코딩·전송하므로 jpg 가 실제 바이트와 정합한다.
        private const val DEFAULT_IMAGE_EXTENSION = "jpg"
    }
}
