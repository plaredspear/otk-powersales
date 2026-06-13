package com.otoki.powersales.external.sf.outbound

import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import kotlin.collections.get

/**
 * [SfOutboundClient] 운영 구현 — RestClient 기반 (Spring 6 / Boot 4 패턴 정합).
 *
 * 401 재시도: SfOAuthTokenManager 와 협력. 첫 호출 401 → invalidate → 재발급 → 재호출.
 * 재호출에서도 401 면 [SfOAuthFailedException] 발생.
 */
@Component
class SfOutboundClientImpl(
    private val properties: SfOutboundProperties,
    private val tokenManager: SfOAuthTokenManager,
    @Qualifier("sfOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) : SfOutboundClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun callApi(endpoint: String, apiMap: Map<String, Any?>): SfApiResponse {
        if (properties.apexBaseUrl.isBlank()) {
            throw SfOAuthFailedException("apex base URL 미설정 — sf.outbound.apex-base-url 환경변수 확인")
        }
        val url = properties.apexBaseUrl.trimEnd('/') + endpoint
        val jsonBody = objectMapper.writeValueAsString(apiMap)

        return try {
            exchange(url, jsonBody)
        } catch (e: HttpClientErrorException.Unauthorized) {
            log.info("[sf-outbound] 401 응답 — 토큰 재발급 후 1회 재시도")
            tokenManager.invalidateToken()
            try {
                exchange(url, jsonBody)
            } catch (retry: HttpClientErrorException.Unauthorized) {
                throw SfOAuthFailedException("재발급 후에도 401: ${retry.responseBodyAsString}")
            }
        }
    }

    private fun exchange(url: String, jsonBody: String): SfApiResponse {
        val response = restClient.post()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, tokenManager.getAccessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(jsonBody)
            .retrieve()
            .toEntity(String::class.java)

        val body = response.body ?: ""
        if (response.statusCode != HttpStatus.OK) {
            log.warn("[sf-outbound] non-200 응답: status={} body={}", response.statusCode, body)
        }
        return parse(body)
    }

    private fun parse(body: String): SfApiResponse {
        if (body.isBlank()) {
            return SfApiResponse(resultCode = "0", resultMsg = "응답 본문 비어있음", rawBody = body)
        }
        val parsed = objectMapper.readValue(body, Map::class.java)
        val resultCode = parsed["RESULT_CODE"]?.toString() ?: "0"
        val resultMsg = parsed["RESULT_MSG"]?.toString() ?: ""
        return SfApiResponse(resultCode = resultCode, resultMsg = resultMsg, rawBody = body)
    }
}
