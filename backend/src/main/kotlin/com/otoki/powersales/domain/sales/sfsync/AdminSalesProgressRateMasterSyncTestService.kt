package com.otoki.powersales.domain.sales.sfsync

import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * SF Apex REST `/IF_salesprogresssend` 거래처목표등록마스터 조회 테스트 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * `MOD_DT`(기준 일자) 하나를 SF 로 POST 하면 SF 가 해당 일자 기준으로 변경된 거래처목표등록마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스 ("알라딘 거래처목표 마스터 API" 문서 정합). SF 응답 원형을 그대로
 * 반환하며, 요청 `save=true` 면 그 응답을 주기 sync 와 동일 경로([SalesProgressRateMasterSyncService.syncRecords],
 * ExternalKey upsert)로 신규 DB 에 저장하고 통계를 함께 담는다. `save=false`(기본) 는 조회 전용 — DB 변경 없음.
 *
 * 처리:
 *  1. `{ "MOD_DT": modDt }` apiMap 구성.
 *  2. [SfOutboundClient.callApi] 로 SF POST — 클레임 등록과 동일한 OAuth/401 재시도 경로 재사용.
 *  3. `save=true` + SF 호출 성공 시 rawBody 를 [SalesProgressRateMasterFetchClient.parse] 로 재해석해
 *     upsert — SF 재호출 없이 응답 1회분을 raw 노출과 저장에 공유.
 *  4. 성공/실패 모두 응답에 담아 반환 (예외 throw 안 함).
 *
 * 조회 전용 경로는 audit 적재 없이 slf4j INFO 로그만 남긴다. 저장 경로의 DB 반영 내역은
 * upsert 통계([AdminSalesProgressRateMasterSyncTestResponse.syncResult])로 노출.
 */
@Service
class AdminSalesProgressRateMasterSyncTestService(
    private val sfOutboundClient: SfOutboundClient,
    private val objectMapper: ObjectMapper,
    private val fetchClient: SalesProgressRateMasterFetchClient,
    private val syncService: SalesProgressRateMasterSyncService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminSalesProgressRateMasterSyncTestRequest,
    ): AdminSalesProgressRateMasterSyncTestResponse {
        val apiMap = mapOf<String, Any?>("MOD_DT" to request.modDt)
        val requestPayload = objectMapper.writeValueAsString(apiMap)

        val response = try {
            val apiResponse = sfOutboundClient.callApi(SF_ENDPOINT, apiMap)
            val syncResult =
                if (request.save && apiResponse.isSuccess()) syncToDb(apiResponse.rawBody) else null
            AdminSalesProgressRateMasterSyncTestResponse(
                success = apiResponse.isSuccess(),
                resultCode = apiResponse.resultCode,
                resultMsg = apiResponse.resultMsg,
                rawResponse = apiResponse.rawBody,
                requestPayload = requestPayload,
                syncResult = syncResult,
            )
        } catch (e: SfOAuthFailedException) {
            log.warn("[sales-progress-master-sync-test] SF OAuth 실패: {}", e.message)
            AdminSalesProgressRateMasterSyncTestResponse(
                success = false,
                resultCode = null,
                resultMsg = e.message ?: "SF OAuth 실패",
                rawResponse = null,
                requestPayload = requestPayload,
            )
        } catch (e: Exception) {
            log.warn("[sales-progress-master-sync-test] SF 호출 예외: {}", e.message)
            AdminSalesProgressRateMasterSyncTestResponse(
                success = false,
                resultCode = null,
                resultMsg = e.message ?: e.javaClass.simpleName,
                rawResponse = null,
                requestPayload = requestPayload,
            )
        }

        log.info(
            "SF_SALES_PROGRESS_MASTER_SYNC_TEST user={} modDt={} save={} success={} resultCode={} syncResult={}",
            userId, request.modDt, request.save, response.success, response.resultCode, response.syncResult,
        )
        return response
    }

    /** SF 응답 rawBody 를 파싱해 주기 sync 와 동일 경로로 upsert 하고 통계를 반환. */
    private fun syncToDb(rawBody: String?): AdminSalesProgressRateMasterSyncTestResponse.SyncSummary {
        val dtos = fetchClient.parse(rawBody)
        val result = syncService.syncRecords(dtos)
        return AdminSalesProgressRateMasterSyncTestResponse.SyncSummary(
            fetched = result.fetched,
            inserted = result.inserted,
            updated = result.updated,
            skipped = result.skipped,
        )
    }

    companion object {
        /** SF Apex REST suffix (apex base URL 뒤에 붙는다). */
        const val SF_ENDPOINT = "/IF_salesprogresssend"
    }
}
