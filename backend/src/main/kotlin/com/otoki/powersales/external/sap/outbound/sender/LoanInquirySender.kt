package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.domain.activity.order.exception.LoanSapErrorException
import com.otoki.powersales.domain.activity.order.exception.LoanSapHtmlResponseException
import com.otoki.powersales.domain.activity.order.exception.LoanSapUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

/**
 * SAP `LoanInquiry` 동기 호출 sender (Spec #594).
 *
 * 레거시 `IF_REST_MOBILE_LoanInquiry.cls:50-51` 동등 — `{"request": {"SAPAccountCode": "<external_key>"}}`
 * 한 단계 감싼 페이로드로 SAP REST Adapter 호출. `SapResponseHtmlGuard` 로 HTML 응답 차단.
 *
 * 캐시 없음 — 여신 잔액 실시간 정확도 우선 (레거시 동등 — `OrderController.java:380, 797` 매 호출 직호출).
 */
@Component
class LoanInquirySender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(LoanInquirySender::class.java)

    /**
     * SAP `LoanInquiry` 호출.
     *
     * @param externalKey SF `Account.ExternalKey__c` ≡ 신규 `account.external_key` ≡ SAP 거래처 코드
     * @return SAP 응답 파싱 결과
     */
    fun inquire(externalKey: String): LoanInquirySapResult {
        val interfaceId = SapConstants.SAP_INTERFACE_LOAN_INQUIRY
        val endpointPath = "/$interfaceId"
        val payload = mapOf("request" to mapOf("SAPAccountCode" to externalKey))

        val body: String? = try {
            restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)
                .body
        } catch (ex: HttpStatusCodeException) {
            log.warn("SAP LoanInquiry HTTP {} interfaceId={}", ex.statusCode, interfaceId)
            throw LoanSapUnavailableException("SAP HTTP ${ex.statusCode.value()}")
        } catch (ex: ResourceAccessException) {
            log.warn("SAP LoanInquiry 네트워크 실패 interfaceId={} cause={}", interfaceId, ex.message)
            throw LoanSapUnavailableException("SAP 네트워크 오류")
        }

        if (!SapResponseHtmlGuard.isValid(body)) {
            log.warn("SAP LoanInquiry HTML 응답 감지 interfaceId={} bodyHead={}", interfaceId, body?.take(80))
            throw LoanSapHtmlResponseException()
        }
        if (body.isNullOrBlank()) {
            throw LoanSapErrorException("SAP 응답 본문 비어 있음")
        }

        val parsed = try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            log.warn("SAP LoanInquiry 응답 JSON 파싱 실패 bodyHead={}", body.take(80))
            throw LoanSapErrorException("SAP 응답 JSON 파싱 실패")
        }

        val resultCode = parsed["resultCode"]?.asString()
        // SAP 응답 원문(resutlMsg 는 SAP 스펙상 오타 필드명) — SAP 오류 문구를 그대로 relay 하되,
        // 어느 SAP 인터페이스에서 난 오류인지 식별 가능하도록 prefix 만 덧붙인다(레거시 relay 정합 유지).
        val resultMsg = parsed["resutlMsg"]?.asString() ?: parsed["resultMsg"]?.asString()
        if (resultCode != "S") {
            throw LoanSapErrorException(withSapPrefix(resultMsg))
        }

        val result = parsed["result"] ?: throw LoanSapErrorException("SAP 응답 result 필드 누락")
        return LoanInquirySapResult(
            totalCredit = parseAmount(result, "TotalCredit"),
            creditBalance = parseAmount(result, "CreditBalance"),
            currency = result["CreditCurrency"]?.asString(),
        )
    }

    private fun parseAmount(node: JsonNode, field: String): BigDecimal? {
        val v = node[field]?.asString()
        if (v.isNullOrBlank()) return null
        return try {
            BigDecimal(v)
        } catch (_: NumberFormatException) {
            null
        }
    }

    companion object {
        /** SAP 여신조회 오류임을 식별하기 위한 접두어. */
        private const val SAP_ERROR_PREFIX = "[SAP여신조회]"

        /**
         * SAP 오류 원문에 인터페이스 식별 prefix 를 덧붙인다.
         * 원문이 없으면(null/공란) prefix 만 붙이지 않고 null 을 반환해
         * [LoanSapErrorException] 의 기본 문구가 쓰이도록 한다.
         */
        private fun withSapPrefix(resultMsg: String?): String? {
            val trimmed = resultMsg?.trim()
            return if (trimmed.isNullOrEmpty()) resultMsg else "$SAP_ERROR_PREFIX $trimmed"
        }
    }
}

data class LoanInquirySapResult(
    val totalCredit: BigDecimal?,
    val creditBalance: BigDecimal?,
    val currency: String?,
)
