package com.otoki.powersales.external.sf.outbound

import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOAuthTokenManager
import com.otoki.powersales.external.sf.outbound.SfOutboundProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@DisplayName("SfOAuthTokenManager 테스트")
class SfOAuthTokenManagerTest {

    private fun newProperties() = SfOutboundProperties(
        apexBaseUrl = "https://sf.example.com/services/apexrest/mobile",
        oauth = SfOutboundProperties.OAuthProps(
            tokenUrl = "https://login.example.com/services/oauth2/token",
            clientId = "cid",
            clientSecret = "secret",
            username = "user",
            password = "pass",
        ),
    )

    private fun mockRestClientReturning(body: Map<*, *>?): RestClient {
        val restClient: RestClient = mockk()
        val requestBodyUriSpec: RestClient.RequestBodyUriSpec = mockk()
        val requestBodySpec: RestClient.RequestBodySpec = mockk()
        val responseSpec: RestClient.ResponseSpec = mockk()
        every { restClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.contentType(any()) } returns requestBodySpec
        every { requestBodySpec.body(any<Any>()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.body(Map::class.java) } returns body
        return restClient
    }

    private fun mockRestClientThrowing(ex: Exception): RestClient {
        val restClient: RestClient = mockk()
        val requestBodyUriSpec: RestClient.RequestBodyUriSpec = mockk()
        val requestBodySpec: RestClient.RequestBodySpec = mockk()
        val responseSpec: RestClient.ResponseSpec = mockk()
        every { restClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.contentType(any()) } returns requestBodySpec
        every { requestBodySpec.body(any<Any>()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.body(Map::class.java) } throws ex
        return restClient
    }

    @Test
    @DisplayName("최초 호출 — token endpoint 호출 후 Bearer + access_token 반환")
    fun firstCallIssuesToken() {
        val restClient = mockRestClientReturning(mapOf("access_token" to "abc123", "token_type" to "Bearer"))
        val manager = SfOAuthTokenManager(newProperties(), restClient)

        val header = manager.getAccessToken()

        assertThat(header).isEqualTo("Bearer abc123")
    }

    @Test
    @DisplayName("두 번째 호출 — 캐시 hit, token endpoint 미호출")
    fun cacheHit() {
        val restClient = mockRestClientReturning(mapOf("access_token" to "abc123", "token_type" to "Bearer"))
        val manager = SfOAuthTokenManager(newProperties(), restClient)

        manager.getAccessToken()
        manager.getAccessToken()

        verify(exactly = 1) { restClient.post() }
    }

    @Test
    @DisplayName("invalidateToken 후 — 재호출 시 token endpoint 다시 호출")
    fun reissueAfterInvalidate() {
        val restClient = mockRestClientReturning(mapOf("access_token" to "abc123", "token_type" to "Bearer"))
        val manager = SfOAuthTokenManager(newProperties(), restClient)

        manager.getAccessToken()
        manager.invalidateToken()
        manager.getAccessToken()

        verify(exactly = 2) { restClient.post() }
    }

    @Test
    @DisplayName("token endpoint 401 → SfOAuthFailedException")
    fun tokenEndpointUnauthorized() {
        val ex = RestClientResponseException(
            "Unauthorized",
            HttpStatusCode.valueOf(401),
            "Unauthorized",
            null,
            "invalid credentials".toByteArray(),
            null,
        )
        val restClient = mockRestClientThrowing(ex)
        val manager = SfOAuthTokenManager(newProperties(), restClient)

        assertThatThrownBy { manager.getAccessToken() }
            .isInstanceOf(SfOAuthFailedException::class.java)
            .hasMessageContaining("401")
    }

    @Test
    @DisplayName("응답에 access_token 부재 → SfOAuthFailedException")
    fun missingAccessToken() {
        val restClient = mockRestClientReturning(mapOf("token_type" to "Bearer"))
        val manager = SfOAuthTokenManager(newProperties(), restClient)

        assertThatThrownBy { manager.getAccessToken() }
            .isInstanceOf(SfOAuthFailedException::class.java)
            .hasMessageContaining("access_token")
    }

    @Test
    @DisplayName("token URL 미설정 → SfOAuthFailedException")
    fun missingTokenUrl() {
        val restClient: RestClient = mockk()
        val properties = SfOutboundProperties(oauth = SfOutboundProperties.OAuthProps(tokenUrl = ""))
        val manager = SfOAuthTokenManager(properties, restClient)

        assertThatThrownBy { manager.getAccessToken() }
            .isInstanceOf(SfOAuthFailedException::class.java)
            .hasMessageContaining("token URL 미설정")
    }
}
