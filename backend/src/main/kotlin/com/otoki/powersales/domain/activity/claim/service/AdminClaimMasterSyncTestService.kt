package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimMasterSyncTestRequest
import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimMasterSyncTestResponse
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
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
 * SF Apex REST `/IF_SendClaimToPWS` 클레임 마스터 조회 + 갱신 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * `MOD_DT`(기준 일자) 하나를 SF 로 POST 하면 SF 가 해당 일자 기준으로 변경된 클레임 마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스. 응답의 각 레코드를 `pwrskey`(=claim_id, 신규 생성분) 우선,
 * 없으면 `Name`(접수번호, SF 생성분) 으로 신규 claim 과 매칭해 **조치/상담 필드를 신규 데이터로 갱신**한다.
 *
 * 갱신 대상 필드(6개) — SF 레거시 inbound Apex(`IF_ClaimStatusUpdate` / `IF_REST_SAP_ClaimReceive`)
 * 가 claim 을 update 로 set 하는 필드 집합과 정합:
 *   actionStatus / actionCode / counselNumber / reasonType / actContent / cosmosKey.
 * 등록 시 확정 필드(제품/거래처/수량/금액 등) 는 갱신하지 않는다. SF 레거시에서도 inbound 갱신은
 * `ClaimTrigger` 의 'Interface' 유저 가드로 상태 검증을 우회하므로, 본 갱신도 claim status 와 무관하게 적용한다.
 *
 * 처리:
 *  1. `{ "MOD_DT": modDt }` 로 SF POST (클레임 등록과 동일한 OAuth/401 재시도 경로).
 *  2. 응답 rawBody(JSON)에서 클레임 레코드 배열 추출 (wrapper key 후보 탐색).
 *  3. 각 레코드 pwrskey → (없으면) name → claim 조회 → 존재하면 6필드 갱신, 없으면 건너뜀(카운트 집계).
 *
 * SF 호출 실패(OAuth/HTTP/파싱)는 throw 하지 않고 실패 응답으로 흡수한다. 갱신은 트랜잭션 안에서 일괄 처리.
 */
@Service
class AdminClaimMasterSyncTestService(
    private val sfOutboundClient: SfOutboundClient,
    private val claimRepository: ClaimRepository,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminClaimMasterSyncTestRequest,
    ): AdminClaimMasterSyncTestResponse {
        val apiMap = mapOf<String, Any?>("MOD_DT" to request.modDt)
        val requestPayload = objectMapper.writeValueAsString(apiMap)

        val apiResponse = try {
            sfOutboundClient.callApi(SF_ENDPOINT, apiMap)
        } catch (e: SfOAuthFailedException) {
            log.warn("[claim-master-sync] SF OAuth 실패: {}", e.message)
            return failure(requestPayload, e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[claim-master-sync] SF 호출 예외: {}", e.message)
            return failure(requestPayload, e.message ?: e.javaClass.simpleName)
        }

        val records = parseRecords(apiResponse.rawBody)
        val result = applyUpdates(records)

        log.info(
            "SF_CLAIM_MASTER_SYNC user={} modDt={} resultCode={} fetched={} updated={} notFound={} skipped={}",
            userId, request.modDt, apiResponse.resultCode,
            result.fetched, result.updated, result.notFound, result.skipped,
        )

        return AdminClaimMasterSyncTestResponse(
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
     * 주기 배치 진입점 — SF fetch → pwrskey(우선)/name(fallback) 매칭 claim 갱신.
     * (통합 잡 [com.otoki.powersales.platform.batch.ClaimMasterSyncBatch] 의 claim 도메인 —
     *  오케스트레이터 [com.otoki.powersales.platform.batch.ClaimMasterSyncBatchService] 가 호출)
     *
     * 테스트 도구([test])와 동일한 fetch/parse/갱신 경로를 쓰되, 응답 DTO 대신 [UpdateResult] 를 반환한다.
     * SF 호출 실패는 흡수하지 않고 throw 하며, 통합 잡의 오케스트레이터가 도메인별 [runCatching] 으로 잡아
     * metadata `error=true` 로 기록한다(다른 도메인 처리는 계속). 배치 자체는 SUCCESS 로 남고 실패는 metadata
     * 로 가시화된다 — 한 도메인 실패가 잡 전체를 FAILURE 로 끌고 가지 않도록 한 격리 설계.
     *
     * @param context 개별 호출 시 실행 통계를 [ScheduledJobRunContext] metadata 로 기록. 통합 잡은 도메인별
     *   집계를 오케스트레이터가 단일 row 로 묶어 기록하므로 null 로 호출한다(개별 서비스의 metadata 미사용).
     * @param modDt SF 조회 기준 일자 (YYYYMMDD). 미지정 시 오늘 — 주기 배치가 당일 변경분을 가져온다.
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
            "[claim-master-sync] 배치 완료 — modDt={} resultCode={} fetched={} updated={} notFound={} skipped={}",
            modDt, apiResponse.resultCode, result.fetched, result.updated, result.notFound, result.skipped,
        )
        return result
    }

    /**
     * 파싱된 레코드들을 한 트랜잭션에서 claim 에 적용한다 (SF I/O 는 트랜잭션 밖 — [test]/[sync] 에서 호출 완료 후 진입).
     *
     * 매칭 키 우선순위:
     *  1. `pwrskey`(=claim_id) — 신규 시스템에서 생성해 SF 가 echo 한 PK. 존재하면 이 값으로 우선 조회.
     *  2. `name`(접수번호, EXNUM) — SF 단독 생성분(pwrskey 미보유)의 자연키. pwrskey 로 찾지 못하면 name 으로 조회.
     *
     * 두 키 모두 없으면 skipped, 키는 있으나 매칭 claim 이 없으면 notFound, 찾으면 6필드를 신규 데이터로 갱신한다.
     */
    fun applyUpdates(records: List<ClaimMasterSfRecord>): UpdateResult = txTemplate.execute {
        var updated = 0
        var notFound = 0
        var skipped = 0

        for (record in records) {
            val claimId = record.pwrskeyAsClaimId()
            val claimName = record.nameAsClaimName()
            if (claimId == null && claimName == null) {
                // pwrskey(신규 PK) 도 name(SF 접수번호) 도 없으면 매칭 불가 — 건너뜀.
                skipped++
                continue
            }
            // pwrskey(신규 시스템 생성) 우선, 없으면 name(SF 생성 접수번호) 으로 fallback 조회.
            val claim = claimId?.let { claimRepository.findByIdOrNull(it) }
                ?: claimName?.let { claimRepository.findByName(it) }
            if (claim == null) {
                notFound++
                continue
            }
            // SF 레거시 inbound SET 절 정합 — 조치/상담 6필드만 신규 데이터로 갱신.
            claim.actionStatus = record.actionStatus
            claim.actionCode = record.actionCode
            claim.counselNumber = record.counselNumber
            claim.reasonType = record.reasonType
            claim.actContent = record.actContent
            // CosmosKey 는 문서 응답 스키마에 없을 수 있다 — 응답에 키가 없으면 null 로 와 기존값을 덮는다.
            // 레거시 IF_REST_SAP_ClaimReceive 도 무조건 set 하므로 동일하게 응답값으로 갱신한다.
            claim.cosmosKey = record.cosmosKey
            updated++
        }

        UpdateResult(fetched = records.size, updated = updated, notFound = notFound, skipped = skipped)
    }!!

    /** rawBody JSON 에서 클레임 레코드 배열을 추출 후 역직렬화. 형식 불명/파싱 실패 시 빈 리스트. */
    private fun parseRecords(raw: String?): List<ClaimMasterSfRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = objectMapper.readTree(raw)
            val arrayNode = SfResponseArrayExtractor.extractArrayNode(root) ?: run {
                log.warn("[claim-master-sync] SF 응답에서 레코드 배열을 찾지 못함 — 빈 리스트. body 앞부분={}", raw.take(200))
                return emptyList()
            }
            objectMapper.convertValue(arrayNode, object : TypeReference<List<ClaimMasterSfRecord>>() {})
        } catch (e: Exception) {
            log.warn("[claim-master-sync] SF 응답 파싱 실패 — 빈 리스트. error={} body 앞부분={}", e.message, raw.take(200))
            emptyList()
        }
    }

    private fun failure(requestPayload: String, message: String) = AdminClaimMasterSyncTestResponse(
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
        const val SF_ENDPOINT = "/IF_SendClaimToPWS"

        /** SF Request Body MOD_DT 형식 (YYYYMMDD). */
        private val MOD_DT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
