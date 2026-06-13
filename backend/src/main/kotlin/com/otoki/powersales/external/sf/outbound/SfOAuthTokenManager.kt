package com.otoki.powersales.external.sf.outbound

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.get

/**
 * SF OAuth 2.0 password grant 토큰 캐시 (Spec #829).
 *
 * - `getAccessToken()` 가 in-memory `AtomicReference<TokenSnapshot>` 에 캐시된 토큰을 반환.
 * - 캐시가 비어 있으면 token endpoint 에 password grant 요청 후 응답을 캐싱.
 * - 401 응답 시 `invalidateToken()` 호출 후 재요청 (1회). 재요청도 실패하면 호출자 (SfOutboundClient) 가
 *   [SfOAuthFailedException] 을 발생시킨다.
 *
 * 캐시 정책: 명시적 expiry 체크 없음 — 레거시 Heroku `ApiService` 정합. 401 응답을 무효화 트리거로 사용.
 *
 * 단일 SF API 사용자 토큰을 모든 backend 호출자가 공유 (Q10 채택).
 */
@Component
class SfOAuthTokenManager(
    private val properties: SfOutboundProperties,
    @Qualifier("sfOutboundRestClient") private val restClient: RestClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = AtomicReference<TokenSnapshot?>(null)

    /**
     * 캐시된 토큰 반환. 비어 있으면 token endpoint 호출 후 캐싱.
     *
     * 반환값: `${tokenType} ${accessToken}` (예: `Bearer abc...`) — Authorization 헤더 그대로 사용.
     */
    fun getAccessToken(): String {
        val cached = cache.get()
        if (cached != null) return cached.toAuthorizationHeader()

        val fresh = issueToken()
        cache.set(fresh)
        return fresh.toAuthorizationHeader()
    }

    /** 401 응답 시 호출 — 다음 `getAccessToken()` 이 강제로 재발급한다. */
    fun invalidateToken() {
        cache.set(null)
    }

    private fun issueToken(): TokenSnapshot {
        if (properties.oauth.tokenUrl.isBlank()) {
            throw SfOAuthFailedException("token URL 미설정 — sf.outbound.oauth.token-url 환경변수 확인")
        }
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("client_id", properties.oauth.clientId)
            add("client_secret", properties.oauth.clientSecret)
            add("username", properties.oauth.username)
            add("password", properties.oauth.password)
        }

        val response: Map<*, *>? = try {
            restClient.post()
                .uri(properties.oauth.tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map::class.java)
        } catch (e: RestClientResponseException) {
            log.warn("[sf-oauth] token endpoint 호출 실패: status={} body={}", e.statusCode, e.responseBodyAsString)
            throw SfOAuthFailedException("token endpoint 응답 ${e.statusCode.value()}: ${e.responseBodyAsString}")
        } catch (e: Exception) {
            log.warn("[sf-oauth] token endpoint 호출 예외: {}", e.message)
            throw SfOAuthFailedException(e.message ?: e.javaClass.simpleName)
        }

        val accessToken = response?.get("access_token")?.toString()
            ?: throw SfOAuthFailedException("응답에 access_token 부재")
        val tokenType = response["token_type"]?.toString() ?: "Bearer"
        return TokenSnapshot(accessToken = accessToken, tokenType = tokenType)
    }

    internal data class TokenSnapshot(
        val accessToken: String,
        val tokenType: String,
    ) {
        fun toAuthorizationHeader(): String = "$tokenType $accessToken"
    }
}
