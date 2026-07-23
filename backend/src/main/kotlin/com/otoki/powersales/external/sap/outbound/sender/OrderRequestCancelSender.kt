package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

/**
 * SAP `OrderChange` 동기 호출 sender (Spec #597 — 주문 취소).
 *
 * 레거시 `IF_REST_MOBILE_OrderCancelRequest.cls:99` `IF_Util.httpCall('IF_REST_SAP_OrderChange', ...)` 동등 —
 * 동기 callout (`@future` 아님). `SapResponseHtmlGuard` 로 HTML 응답 차단.
 *
 * 응답 `resultCode = 'S'` 만 성공으로 간주, 그 외 모두 [OrderCancelSapFailedException] 으로 변환.
 * DB 트랜잭션 외부에서 호출되어야 하며, 응답 'S' 확인 후 호출자가 별도 트랜잭션을 시작한다 (spec.md §6.3).
 *
 * 실패 확실성 구분: HTTP 200 + `resultCode != 'S'`(명시적 거부)만 `rejected=true` 로 던진다. timeout /
 * 네트워크 / HTTP 4xx·5xx / 형식 오류 등 결과 불확실 케이스는 `rejected=false`. 자세한 처리는
 * [OrderCancelSapFailedException] 주석 참조.
 */
@Component
class OrderRequestCancelSender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(OrderRequestCancelSender::class.java)

    fun send(payload: Map<String, Any?>) {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL
        val endpointPath = "/$interfaceId"

        val body: String? = try {
            restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)
                .body
        } catch (ex: HttpStatusCodeException) {
            log.warn("sap.outbound.cancel.failure HTTP={} interfaceId={}", ex.statusCode, interfaceId)
            throw OrderCancelSapFailedException("SAP HTTP ${ex.statusCode.value()}")
        } catch (ex: ResourceAccessException) {
            log.warn("sap.outbound.cancel.failure 네트워크 실패 interfaceId={} cause={}", interfaceId, ex.message)
            throw OrderCancelSapFailedException("SAP 네트워크 오류")
        }

        if (!SapResponseHtmlGuard.isValid(body)) {
            log.warn(
                "sap.outbound.cancel.failure HTML 응답 감지 interfaceId={} bodyHead={}",
                interfaceId, body?.take(80),
            )
            throw OrderCancelSapFailedException("SAP 응답 형식 오류")
        }
        if (body.isNullOrBlank()) {
            throw OrderCancelSapFailedException("SAP 응답 본문 비어 있음")
        }

        val parsed = try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            log.warn("sap.outbound.cancel.failure 응답 JSON 파싱 실패 bodyHead={}", body.take(80))
            throw OrderCancelSapFailedException("SAP 응답 JSON 파싱 실패")
        }

        val resultCode = parsed["resultCode"]?.asString()
        val resultMsg = parsed["resutlMsg"]?.asString() ?: parsed["resultMsg"]?.asString()
        if (resultCode != "S") {
            // HTTP 200 + resultCode != 'S' = SAP 가 요청을 받고 취소를 명시적으로 거부 (결과 확실).
            // rejected=true 로 표기해 호출자가 취소요청 흔적을 롤백하게 한다.
            log.warn("sap.outbound.cancel.failure 명시적 거부 interfaceId={} resultCode={} msg={}", interfaceId, resultCode, resultMsg)
            throw OrderCancelSapFailedException(
                resultMsg ?: "SAP 응답 오류 (resultCode=$resultCode)",
                rejected = true,
            )
        }
    }
}
