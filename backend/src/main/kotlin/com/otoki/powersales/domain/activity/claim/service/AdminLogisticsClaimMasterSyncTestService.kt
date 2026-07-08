package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.AdminLogisticsClaimMasterSyncTestRequest
import com.otoki.powersales.domain.activity.claim.dto.response.AdminLogisticsClaimMasterSyncTestResponse
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfResponseArrayExtractor
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SF Apex REST `/IF_SendLogisticsClaimToPWS` 물류 클레임 마스터 조회 + 갱신 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * `MOD_DT`(기준 일자) 하나를 SF 로 POST 하면 SF 가 해당 일자 기준으로 변경된 물류 클레임(제안) 마스터 목록을
 * 응답한다. 응답의 각 레코드를 `pwrskey`(=suggestion_id) 로 신규 제안([com.otoki.powersales.domain.activity.suggestion.entity.Suggestion])과
 * 매칭해 **조치 계열 6필드를 신규 데이터로 갱신**한다 (일반 클레임 마스터 조회([AdminClaimMasterSyncTestService]) 와 동일 패턴).
 *
 * 갱신 대상 6필드 — 물류클레임은 레거시에 외부 갱신 inbound Apex 가 없어(조치필드는 SF UI 직접입력), 조회 REST
 * (`IF_REST_MOBILE_LogisticsClaimSearch`) 응답의 **등록 시 미설정 + 나중에 채워지는 조치 계열 필드** 를 권위로 삼는다:
 *   actionNum / actionStatus / actionManager / logisticsResponsibility / claimTypeMeasures / actionContent.
 * 제목/상세내용/거래처/제품 등 등록 시 확정 필드는 갱신하지 않는다.
 *
 * 처리:
 *  1. `{ "MOD_DT": modDt }` 로 SF POST (클레임 등록과 동일한 OAuth/401 재시도 경로).
 *  2. 응답 rawBody(JSON)에서 레코드 배열 추출 (wrapper key 후보 탐색).
 *  3. 각 레코드 pwrskey → suggestion 조회 → 존재하면 6필드 갱신, 없으면 건너뜀(카운트 집계).
 *
 * SF 호출 실패(OAuth/HTTP/파싱)는 throw 하지 않고 실패 응답으로 흡수한다. 갱신은 트랜잭션 안에서 일괄 처리.
 */
@Service
class AdminLogisticsClaimMasterSyncTestService(
    private val sfOutboundClient: SfOutboundClient,
    private val suggestionRepository: SuggestionRepository,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminLogisticsClaimMasterSyncTestRequest,
    ): AdminLogisticsClaimMasterSyncTestResponse {
        val apiMap = mapOf<String, Any?>("MOD_DT" to request.modDt)
        val requestPayload = objectMapper.writeValueAsString(apiMap)

        val apiResponse = try {
            sfOutboundClient.callApi(SF_ENDPOINT, apiMap)
        } catch (e: SfOAuthFailedException) {
            log.warn("[logistics-claim-master-sync] SF OAuth 실패: {}", e.message)
            return failure(requestPayload, e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[logistics-claim-master-sync] SF 호출 예외: {}", e.message)
            return failure(requestPayload, e.message ?: e.javaClass.simpleName)
        }

        val records = parseRecords(apiResponse.rawBody)
        val result = applyUpdates(records)

        log.info(
            "SF_LOGISTICS_CLAIM_MASTER_SYNC user={} modDt={} resultCode={} fetched={} updated={} notFound={} skipped={}",
            userId, request.modDt, apiResponse.resultCode,
            result.fetched, result.updated, result.notFound, result.skipped,
        )

        return AdminLogisticsClaimMasterSyncTestResponse(
            success = true,
            resultCode = apiResponse.resultCode,
            resultMsg = apiResponse.resultMsg,
            rawResponse = apiResponse.rawBody,
            requestPayload = requestPayload,
            fetchedCount = result.fetched,
            updatedCount = result.updated,
            notFoundCount = result.notFound,
            skippedCount = result.skipped,
        )
    }

    /**
     * 주기 배치 진입점 — SF fetch → pwrskey 매칭 제안(물류클레임) 갱신.
     * ([com.otoki.powersales.platform.batch.LogisticsClaimMasterSyncBatch])
     *
     * 테스트 도구([test])와 동일한 fetch/parse/갱신 경로를 쓰되, 응답 DTO 대신 [UpdateResult] 를 반환하고
     * 실행 통계를 [ScheduledJobRunContext] metadata 로 기록한다. SF 호출 실패는 throw 하여 배치 러너가
     * FAILURE 로 이력에 남기도록 한다.
     *
     * @param modDt SF 조회 기준 일자 (YYYYMMDD). 미지정 시 오늘.
     */
    fun sync(
        context: ScheduledJobRunContext? = null,
        modDt: String = LocalDate.now().format(MOD_DT_FORMAT),
    ): UpdateResult {
        val apiResponse = sfOutboundClient.callApi(SF_ENDPOINT, mapOf("MOD_DT" to modDt))
        val records = parseRecords(apiResponse.rawBody)
        val result = applyUpdates(records)
        context?.metadata(
            mapOf(
                "fetched" to result.fetched,
                "updated" to result.updated,
                "notFound" to result.notFound,
                "skipped" to result.skipped,
            )
        )
        log.info(
            "[logistics-claim-master-sync] 배치 완료 — modDt={} resultCode={} fetched={} updated={} notFound={} skipped={}",
            modDt, apiResponse.resultCode, result.fetched, result.updated, result.notFound, result.skipped,
        )
        return result
    }

    /**
     * 파싱된 레코드들을 한 트랜잭션에서 suggestion 에 적용한다 (SF I/O 는 트랜잭션 밖).
     * pwrskey(=suggestion_id) 로 제안을 찾아 존재하면 조치 6필드를 신규 데이터로 갱신한다.
     */
    fun applyUpdates(records: List<LogisticsClaimMasterSfRecord>): UpdateResult = txTemplate.execute {
        var updated = 0
        var notFound = 0
        var skipped = 0

        for (record in records) {
            val suggestionId = record.pwrskeyAsSuggestionId()
            if (suggestionId == null) {
                skipped++
                continue
            }
            val suggestion = suggestionRepository.findByIdOrNull(suggestionId)
            if (suggestion == null) {
                notFound++
                continue
            }
            // 조치 계열 6필드만 신규 데이터로 갱신 (등록 확정 필드는 미변경).
            suggestion.actionNum = record.actionNum
            suggestion.actionStatus = record.actionStatusEnum()
            suggestion.actionManager = record.actionManager
            suggestion.logisticsResponsibility = record.logisticsResponsibility
            suggestion.claimTypeMeasures = record.claimTypeMeasures
            suggestion.actionContent = record.actionContent
            updated++
        }

        UpdateResult(fetched = records.size, updated = updated, notFound = notFound, skipped = skipped)
    }!!

    /** rawBody JSON 에서 물류클레임 레코드 배열을 추출 후 역직렬화. 형식 불명/파싱 실패 시 빈 리스트. */
    private fun parseRecords(raw: String?): List<LogisticsClaimMasterSfRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = objectMapper.readTree(raw)
            val arrayNode = SfResponseArrayExtractor.extractArrayNode(root) ?: run {
                log.warn("[logistics-claim-master-sync] SF 응답에서 레코드 배열을 찾지 못함 — 빈 리스트. body 앞부분={}", raw.take(200))
                return emptyList()
            }
            objectMapper.convertValue(arrayNode, object : TypeReference<List<LogisticsClaimMasterSfRecord>>() {})
        } catch (e: Exception) {
            log.warn("[logistics-claim-master-sync] SF 응답 파싱 실패 — 빈 리스트. error={} body 앞부분={}", e.message, raw.take(200))
            emptyList()
        }
    }

    private fun failure(requestPayload: String, message: String) = AdminLogisticsClaimMasterSyncTestResponse(
        success = false,
        resultCode = null,
        resultMsg = message,
        rawResponse = null,
        requestPayload = requestPayload,
    )

    /** 갱신 집계 결과. */
    data class UpdateResult(
        val fetched: Int,
        val updated: Int,
        val notFound: Int,
        val skipped: Int,
    )

    companion object {
        /** SF Apex REST suffix (apex base URL 뒤에 붙는다). */
        const val SF_ENDPOINT = "/IF_SendLogisticsClaimToPWS"

        /** SF Request Body MOD_DT 형식 (YYYYMMDD). */
        private val MOD_DT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
