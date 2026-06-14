package com.otoki.powersales.external.sap.outbox

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import kotlin.collections.get

/**
 * 도메인 무관 범용 SAP outbound 송신 처리 (Spec #592). batch 진입점은 [com.otoki.powersales.platform.batch.SapOutboxBatch].
 *
 * 각 row 의 `interface_id` 로 [SapInterfaceRegistry] 에서 endpoint 조회 후
 * `payload` 그대로 SAP REST Adapter 로 송신. 응답 본문 검증은 [SapResponseHtmlGuard] + `resultCode` 파싱.
 *
 * 응답 결과는 [SapOutboxStatusHandlerRegistry] 가 dispatch 한 도메인 핸들러로 전달되어
 * 도메인 상태(`order_request.order_request_status` 등) 가 갱신된다.
 *
 * **재시도 정책**: 송신 실패 시 [SapOutbox.MAX_RETRY_COUNT] 까지 `RETRY` 상태로 보관, 초과 시 `FAILED`.
 */
@Service
class SapOutboxBatchService(
    private val outboxRepository: SapOutboxRepository,
    private val interfaceRegistry: SapInterfaceRegistry,
    private val statusHandlerRegistry: SapOutboxStatusHandlerRegistry,
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${app.sap.outbox.batch-size:100}") private val batchSize: Int,
) {

    private val log = LoggerFactory.getLogger(SapOutboxBatchService::class.java)

    fun execute(context: ScheduledJobRunContext? = null): WorkerResult {
        val rows = outboxRepository.findPendingOrRetry(PageRequest.of(0, batchSize))
        var sent = 0
        var failed = 0
        rows.forEach { row ->
            val ok = processOne(row.id)
            if (ok) sent++ else failed++
        }
        if (sent > 0 || failed > 0) {
            log.info("SAP outbox 워커 완료 sent=$sent failed=$failed total=${rows.size}")
        }
        context?.metadata(mapOf("picked" to rows.size, "sent" to sent, "failed" to failed))
        return WorkerResult(picked = rows.size, sent = sent, failed = failed)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processOne(outboxId: Long): Boolean {
        val outbox = outboxRepository.findById(outboxId).orElse(null) ?: return false
        if (outbox.status !in setOf(SapOutbox.STATUS_PENDING, SapOutbox.STATUS_RETRY)) {
            return false
        }

        val endpoint = interfaceRegistry.resolveEndpoint(outbox.interfaceId)
        if (endpoint == null) {
            log.error("SAP outbox interfaceId 미등록 outboxId=$outboxId interfaceId=${outbox.interfaceId}")
            outbox.markFailed("UNREGISTERED_INTERFACE_ID")
            outboxRepository.save(outbox)
            statusHandlerRegistry.resolve(outbox.domainType)?.handle(outbox, false, "UNREGISTERED_INTERFACE_ID")
            return false
        }

        val payloadMap = parsePayload(outbox.payload)

        val (success, resultMsg) = try {
            val response = restClient.post()
                .uri(endpoint)
                .body(payloadMap)
                .retrieve()
                .toEntity(String::class.java)

            val body = response.body
            val htmlOk = SapResponseHtmlGuard.isValid(body)
            if (!htmlOk) {
                false to "HTML_RESPONSE_DETECTED"
            } else {
                interpretSapResponse(body)
            }
        } catch (ex: HttpStatusCodeException) {
            false to "HTTP_${ex.statusCode.value()}"
        } catch (ex: ResourceAccessException) {
            false to "NETWORK_ERROR: ${ex.message}"
        } catch (ex: Exception) {
            log.warn("SAP outbox 송신 예외 outboxId=$outboxId", ex)
            false to "UNEXPECTED: ${ex.javaClass.simpleName}"
        }

        if (success) {
            outbox.markSent()
        } else if (outbox.retryCount + 1 >= SapOutbox.MAX_RETRY_COUNT) {
            outbox.markFailed(resultMsg)
        } else {
            outbox.markRetry(resultMsg)
        }
        outboxRepository.save(outbox)

        statusHandlerRegistry.resolve(outbox.domainType)?.handle(outbox, success, resultMsg)
        return success
    }

    private fun parsePayload(payloadJson: String): Map<String, Any?> {
        return try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(payloadJson, Map::class.java) as Map<String, Any?>
        } catch (ex: Exception) {
            log.error("SAP outbox payload JSON 파싱 실패", ex)
            emptyMap()
        }
    }

    private fun interpretSapResponse(body: String?): Pair<Boolean, String?> {
        if (body.isNullOrBlank()) return false to "EMPTY_RESPONSE"
        return try {
            val parsed = objectMapper.readValue(body, Map::class.java)
            val resultCode = parsed["resultCode"]?.toString()
            val resultMsg = parsed["resutlMsg"]?.toString() ?: parsed["resultMsg"]?.toString()
            (resultCode == "S") to resultMsg
        } catch (_: Exception) {
            false to "INVALID_JSON: ${body.take(200)}"
        }
    }

    data class WorkerResult(val picked: Int, val sent: Int, val failed: Int)
}
