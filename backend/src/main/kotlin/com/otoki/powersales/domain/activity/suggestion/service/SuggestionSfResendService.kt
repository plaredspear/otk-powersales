package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 제안/물류클레임 SF 재전송 service — 전송실패(SEND_FAILED) 건 재전송 배치 전용.
 *
 * 등록 흐름([SuggestionService.create]) 은 동기 SF 전송(레거시 정합)을 그대로 유지하고, 본 service 는
 * **실패 후 재전송** 경로만 담당한다. suggestionId 하나로 DB(Suggestion + UploadFile)에서 SF payload 를
 * 복원해 SF `/ProposalRegist` 로 다시 전송하고 sf_send_status 를 전이한다.
 *
 * 설계 정합:
 *  - 클레임 재전송([com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService.dispatch]) 과
 *    동일하게 "snapshot 복원(Tx1) → SF 호출(Tx 밖) → status update(Tx2)" 3단계.
 *  - SF 호출은 트랜잭션 외부에서 일어나며 실패해도 throw 하지 않는다 — suggestion 은 SEND_FAILED 로 보존된다.
 *  - employee/product/account 는 fetch join([SuggestionRepository.findByIdWithSfRefs])으로 즉시 로드해
 *    enhancement 환경의 LAZY 미초기화 함정을 회피한다.
 *
 * 이미지는 레거시가 S3 사전 업로드 후 식별 정보(UniqueKey/FileName/FileSize)만 전송하는 방식이라, 등록 시
 * 저장된 UploadFile row 의 uniqueKey/name/fileSize 를 1·2번 슬롯(최대 2장)에 채운다 — S3 재다운로드 불필요.
 * fileSize 는 등록 시 `formatFileSize()`(레거시 ImageUtil.getFileSize 정합)로 저장된 포맷 문자열 그대로다.
 */
@Service
class SuggestionSfResendService(
    private val suggestionRepository: SuggestionRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val sfOutboundClient: SfOutboundClient,
    private val txTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * suggestionId 기준으로 SF 재전송을 수행하고 sf_send_status 를 전이한다.
     *
     * @param allowedStatuses 재전송 허용 상태(기본 [SuggestionSfSendStatus.SEND_FAILED]). 그 외 상태면 skip.
     * @return SF push 결과. 상태 가드 위반으로 skip 시 null.
     */
    fun resend(
        suggestionId: Long,
        allowedStatuses: Set<SuggestionSfSendStatus> = setOf(SuggestionSfSendStatus.SEND_FAILED),
    ): SfPushResult? {
        // 1. 트랜잭션 안에서 payload snapshot 복원 (Suggestion + UploadFile 이미지 메타).
        val apiMap = txTemplate.execute {
            val suggestion = suggestionRepository.findByIdWithSfRefs(suggestionId)
                ?: throw SuggestionNotFoundException()
            if (suggestion.sfSendStatus !in allowedStatuses) {
                log.warn("제안 SF 재전송 skip — suggestionId={} status={}", suggestionId, suggestion.sfSendStatus)
                return@execute null
            }
            val photoMetas = uploadFileRepository
                .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestionId)
                .filter { !it.uniqueKey.isNullOrBlank() }
                .sortedBy { it.createdAt }
                .take(MAX_PHOTO_SLOTS)
                .map { SfPhotoMeta(uniqueKey = it.uniqueKey!!, fileName = it.name, fileSize = it.fileSize) }
            buildSfApiMap(suggestion, photoMetas)
        } ?: return null

        // 2. SF call (트랜잭션 외부).
        val sfResult = invokeSf(apiMap)

        // 3. Transaction 2 — status update (SENT / SEND_FAILED).
        // Tx1(findByIdWithSfRefs) · targetIds 와 동일하게 삭제 무관 조회로 통일한다.
        // findByIdAndIsDeletedFalse 를 쓰면 soft-delete 된 대상은 여기서 null → 예외로 죽어 applySfResult 가
        // 실행되지 않고, SF 전송은 이미 성공했는데 attemptCount 미증가 + status 미전이로 매 배치마다 무한
        // 중복 전송된다(상한도 무력화). 제품클레임 dispatch(Tx1 findByIdWithSfRefs ↔ Tx2 findByIdOrNull) 정합.
        txTemplate.execute {
            val suggestion = suggestionRepository.findByIdWithSfRefs(suggestionId)
                ?: throw SuggestionNotFoundException()
            applySfResult(suggestion, sfResult)
        }
        return sfResult
    }

    internal fun invokeSf(apiMap: Map<String, Any?>): SfPushResult =
        try {
            val response = sfOutboundClient.callApi(SF_PROPOSAL_REGIST_ENDPOINT, apiMap)
            SfPushResult(success = response.isSuccess(), apiResponse = response, errorSummary = null)
        } catch (e: SfOAuthFailedException) {
            log.warn("[suggestion-sf-resend] SF OAuth 실패: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[suggestion-sf-resend] SF 호출 예외: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: e.javaClass.simpleName)
        }

    internal fun applySfResult(suggestion: Suggestion, result: SfPushResult) {
        suggestion.sfSendAttemptCount += 1
        if (result.success) {
            suggestion.sfSendStatus = SuggestionSfSendStatus.SENT
            suggestion.sfSentAt = LocalDateTime.now()
            suggestion.sfSendFailMessage = null
        } else {
            suggestion.sfSendStatus = SuggestionSfSendStatus.SEND_FAILED
            suggestion.sfSendFailMessage = (result.apiResponse?.resultMsg ?: result.errorSummary)?.take(1000)
        }
    }

    /**
     * 레거시 `IF_REST_MOBILE_ProposalRegist` payload 를 저장된 [Suggestion] + [UploadFile] 로부터 복원한다.
     * 등록 시 동기 전송([SuggestionService] 의 buildSfApiMap, request 기반) 과 동일 key 셋 — 소문자
     * `accountCode` 한 가지, `Type` 미전송, 미입력 값 key 생략. pwrskey 는 suggestion PK(back-link).
     *
     * ⚠️ 재전송 payload 는 "등록 순간의 request 재현" 이 아니라 **현재 DB 상태 기준 재구성**이다. 대부분의
     * key(Title/Description/Category/EmployeeCode/accountCode/claim*)는 등록 시 저장값과 동일 경로라 동등하나,
     * `ProductCode` 는 예외다 — 등록 payload 는 `request.productCode` 원문을 보내지만, 재전송은
     * `suggestion.product?.productCode`(FK 역복원)를 쓴다. 등록 시 productCode 매칭에 실패해 `product=null` 로
     * 저장된 건([SuggestionService.create] step3)은 재전송 payload 에서 `ProductCode` key 가 생략된다.
     * (미등록 productCode 로 등록된 소수 건에 한정 — 정상 매칭 건은 동등.)
     */
    internal fun buildSfApiMap(
        suggestion: Suggestion,
        photoMetas: List<SfPhotoMeta>,
    ): Map<String, Any?> = buildMap {
        put("pwrskey", suggestion.id.toString())
        put("Category", suggestion.category?.displayName)
        put("ProductCode", suggestion.product?.productCode?.trim())
        put("Title", suggestion.title?.trim())
        put("Description", suggestion.content?.trim())
        put("CarNumber", suggestion.carNumber?.trim()?.takeIf { it.isNotEmpty() })
        put("claimList", suggestion.claimType?.trim())
        suggestion.claimDate?.let { put("logclaimDate", it.format(SF_DATE_FMT)) }
        put("EmployeeCode", suggestion.employee?.employeeCode)
        suggestion.sapAccountCode?.trim()?.takeIf { it.isNotEmpty() }?.let { put("accountCode", it) }

        photoMetas.getOrNull(0)?.let {
            put("S3ImageUniqueKey1", it.uniqueKey)
            put("S3ImageFileName1", it.fileName)
            put("S3ImageFileSize1", it.fileSize)
        }
        photoMetas.getOrNull(1)?.let {
            put("S3ImageUniqueKey2", it.uniqueKey)
            put("S3ImageFileName2", it.fileName)
            put("S3ImageFileSize2", it.fileSize)
        }
    }

    /** SF 전송용 첨부 메타 — UploadFile row 복원 (fileSize 는 등록 시 포맷된 문자열 그대로). */
    internal data class SfPhotoMeta(
        val uniqueKey: String,
        val fileName: String?,
        val fileSize: String?,
    )

    /** SF push 결과 — 성공/실패 + 응답 또는 오류 요약 (클레임 등록 정합). */
    data class SfPushResult(
        val success: Boolean,
        val apiResponse: SfApiResponse?,
        val errorSummary: String?,
    )

    companion object {
        /** SF Apex REST `IF_REST_MOBILE_ProposalRegist` endpoint suffix (클레임 `/ClaimRegist` 와 동일 컨벤션). */
        private const val SF_PROPOSAL_REGIST_ENDPOINT = "/ProposalRegist"

        /** SF `S3ImageFileSize1/2` 2개 슬롯만 지원 (레거시 정합). */
        private const val MAX_PHOTO_SLOTS = 2

        /** SF Apex `Date.valueOf(String)` 는 ISO(yyyy-MM-dd) 만 파싱 (클레임 등록 정합). */
        private val SF_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
