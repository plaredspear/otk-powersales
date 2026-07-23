package com.otoki.powersales.external.sap.outbox

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.external.sap.outbound.guard.SapResponseInterpreter
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
            // interfaceId 미등록은 설정 오류(SAP 도달 전) — 확정 거부(rejected)가 아니다.
            statusHandlerRegistry.resolve(outbox.domainType)?.handle(outbox, false, "UNREGISTERED_INTERFACE_ID", false)
            return false
        }

        val payloadMap = parsePayload(outbox.payload)

        // (성공 여부, 메시지, 확정 거부 여부) — rejected=true 는 SAP 가 resultCode 를 반환했고 ≠ 'S' 인 경우로,
        // 재시도해도 결과가 동일하므로 즉시 확정 실패한다. 예외 경로(HTML/HTTP/NETWORK/UNEXPECTED)는 SAP 도달·처리
        // 여부가 불확실해 rejected=false → 종전대로 재시도 대상.
        val outcome: SendOutcome = try {
            val response = restClient.post()
                .uri(endpoint)
                .body(payloadMap)
                .retrieve()
                .toEntity(String::class.java)

            val body = response.body
            val htmlOk = SapResponseHtmlGuard.isValid(body)
            if (!htmlOk) {
                SendOutcome(success = false, message = "HTML_RESPONSE_DETECTED", rejected = false)
            } else {
                val result = SapResponseInterpreter.interpret(objectMapper, body)
                SendOutcome(success = result.success, message = result.message, rejected = result.rejected)
            }
        } catch (ex: HttpStatusCodeException) {
            SendOutcome(success = false, message = "HTTP_${ex.statusCode.value()}", rejected = false)
        } catch (ex: ResourceAccessException) {
            SendOutcome(success = false, message = "NETWORK_ERROR: ${ex.message}", rejected = false)
        } catch (ex: Exception) {
            log.warn("SAP outbox 송신 예외 outboxId=$outboxId", ex)
            SendOutcome(success = false, message = "UNEXPECTED: ${ex.javaClass.simpleName}", rejected = false)
        }

        val (success, resultMsg, rejected) = outcome
        if (success) {
            outbox.markSent()
        } else if (rejected) {
            // SAP 명시적 거부 — 재시도 스킵, 즉시 FAILED 확정 (retryCount 미증가).
            outbox.markRejected(resultMsg)
        } else if (outbox.retryCount + 1 >= SapOutbox.MAX_RETRY_COUNT) {
            outbox.markFailed(resultMsg)
        } else {
            outbox.markRetry(resultMsg)
        }
        outboxRepository.save(outbox)

        statusHandlerRegistry.resolve(outbox.domainType)?.handle(outbox, success, resultMsg, rejected)
        return success
    }

    private data class SendOutcome(val success: Boolean, val message: String?, val rejected: Boolean)

    private fun parsePayload(payloadJson: String): Map<String, Any?> {
        return try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(payloadJson, Map::class.java) as Map<String, Any?>
        } catch (ex: Exception) {
            log.error("SAP outbox payload JSON 파싱 실패", ex)
            emptyMap()
        }
    }

    data class WorkerResult(val picked: Int, val sent: Int, val failed: Int)
}
