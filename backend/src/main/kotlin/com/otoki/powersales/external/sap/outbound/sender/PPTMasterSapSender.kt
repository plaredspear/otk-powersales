package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterSapPayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.LocalDateTime

/**
 * 전문행사조 마스터(PPT_MASTER) SAP 송신 sender (Spec #765).
 *
 * 페이지 단위로 SAP REST Adapter `/SD03300` 에 POST 한다.
 * - interfaceId: [SapConstants.SAP_INTERFACE_PPT_MASTER]
 * - 응답 검증: [SapResponseHtmlGuard] (응답 본문에 `<` 포함 시 실패)
 * - 결과는 [SapOutboundLogService] 로 적재
 *
 * **재시도 없음** — 레거시 `IF_REST_SAP_PPTMToSAP.cls` 가 동기 callout + 실패 시 다음 cron fire (1시간 후)
 * 대기 정책이었으므로 정합. TeamMemberScheduleSapSender / DisplayMasterSapSender 패턴 정합.
 */
@Component
class PPTMasterSapSender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val logService: SapOutboundLogService
) {

    private val log = LoggerFactory.getLogger(PPTMasterSapSender::class.java)

    /**
     * 페이지 1건을 SAP 으로 송신한다.
     * @return 송신 성공 여부 (true = SAP 응답 본문 검증 통과)
     */
    fun sendPage(payload: PPTMasterSapPayload): Boolean {
        if (payload.REQUEST.isEmpty()) {
            // 정상 batch 흐름: 보낼 행이 없으면 SAP 호출 없이 SKIP (멱등). 연결성 확인은 [sendEmptyForConnectivityCheck] 사용.
            val requestedAt = LocalDateTime.now()
            val startNanos = System.nanoTime()
            persistLog(
                interfaceId = SapConstants.SAP_INTERFACE_PPT_MASTER,
                endpointPath = "/${SapConstants.SAP_INTERFACE_PPT_MASTER}",
                requestCount = 0,
                httpStatus = null,
                resultCode = "SKIPPED",
                resultMsg = "EMPTY_PAGE",
                attemptCount = 0,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = null
            )
            return true
        }
        return post(payload)
    }

    /**
     * 빈 배열(`{ "REQUEST": [] }`) 을 **실제로 SAP REST Adapter 로 POST** 한다.
     *
     * 조회 없이 outbound 인터페이스(`SD03300`) 의 연결/응답 정상 여부만 확인하는 용도.
     * [sendPage] 의 빈 배열 SKIP 가드를 우회한다. 결과는 [SapOutboundLogService] 에 동일하게 적재된다.
     * @return 송신 성공 여부 (true = SAP 응답 본문 검증 통과)
     */
    fun sendEmptyForConnectivityCheck(): Boolean = post(PPTMasterSapPayload(REQUEST = emptyList()))

    private fun post(payload: PPTMasterSapPayload): Boolean {
        val interfaceId = SapConstants.SAP_INTERFACE_PPT_MASTER
        val endpointPath = "/$interfaceId"

        // 실제 HTTP 호출 이력은 ExternalApiLogInterceptor → SapOutboundResponseSink 가 sap_outbound_log 로 일괄 적재한다.
        return try {
            val response = restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)

            val status = response.statusCode.value()
            val body = response.body
            val isValid = SapResponseHtmlGuard.isValid(body)
            if (!isValid) {
                log.warn("SAP 전문행사조 마스터 송신 실패 (HTML 응답) interfaceId={} status={} bodyHead={}", interfaceId, status, body?.take(80))
            }
            isValid
        } catch (ex: HttpStatusCodeException) {
            log.warn("SAP 전문행사조 마스터 송신 실패 (HTTP {}) interfaceId={}", ex.statusCode, interfaceId)
            false
        } catch (ex: ResourceAccessException) {
            log.warn("SAP 전문행사조 마스터 송신 실패 (네트워크) interfaceId={} cause={}", interfaceId, ex.message)
            false
        }
    }

    private fun persistLog(
        interfaceId: String,
        endpointPath: String,
        requestCount: Int,
        httpStatus: Int?,
        resultCode: String?,
        resultMsg: String?,
        attemptCount: Int,
        startNanos: Long,
        requestedAt: LocalDateTime,
        errorDetail: String?
    ) {
        try {
            logService.log(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = httpStatus,
                resultCode = resultCode,
                resultMsg = resultMsg,
                attemptCount = attemptCount,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now()
            )
        } catch (ex: Exception) {
            log.error("SAP 전문행사조 마스터 송신 이력 저장 실패 interfaceId=$interfaceId", ex)
        }
    }
}
