package com.otoki.powersales.external.sf.health

import com.otoki.powersales.external.sf.auth.dto.TokenResponse
import tools.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64

/**
 * SF 인바운드 OAuth 전체 흐름 + health endpoint 통합 테스트.
 *
 * (1) /api/v1/sf/oauth/token 으로 token 발급
 * (2) /api/v1/sf/inbound/health 호출 + Authorization: Bearer <token>
 * (3) sf.write scope 검증 통과 → 200 OK
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["sf.auth.allowed-scopes[0]=sf.write"])
@DisplayName("SF 인바운드 OAuth + health endpoint 통합 테스트")
class SfInboundHealthIntegrationTest {

    companion object {
        const val CLIENT_ID = "otoki-sf-it-client"
        const val CLIENT_SECRET = "sf-it-secret"
        private val SECRET_HASH: String = BCryptPasswordEncoder().encode(CLIENT_SECRET)!!

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("sf.auth.client-id") { CLIENT_ID }
            registry.add("sf.auth.client-secret-hash") { SECRET_HASH }
            registry.add("sf.auth.jwt-signing-key") { "sf-integration-test-jwt-signing-key-with-256-bits-1234567890" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private fun issueToken(scope: String = "sf.write"): String {
        val result = mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_id", CLIENT_ID)
                .param("client_secret", CLIENT_SECRET)
                .param("scope", scope)
        ).andExpect(status().isOk).andReturn()
        val response = objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
        assertThat(response.accessToken).isNotBlank()
        return response.accessToken
    }

    @Test
    @DisplayName("토큰 발급 후 health 호출 - 200 OK")
    fun health_with_valid_token() {
        val token = issueToken()

        mockMvc.perform(
            get("/api/v1/sf/inbound/health")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.scope").value("sf.write"))
    }

    @Test
    @DisplayName("토큰 없이 health 호출 - 401")
    fun health_without_token() {
        mockMvc.perform(get("/api/v1/sf/inbound/health"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("잘못된 토큰으로 health 호출 - 401")
    fun health_with_invalid_token() {
        mockMvc.perform(
            get("/api/v1/sf/inbound/health")
                .header("Authorization", "Bearer not-a-valid-jwt")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("토큰 발급 — JSON 본문 형식")
    fun token_issue_json_body() {
        mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/json")
                .content("""{"grant_type":"client_credentials","client_id":"$CLIENT_ID","client_secret":"$CLIENT_SECRET","scope":"sf.write"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.scope").value("sf.write"))
    }

    @Test
    @DisplayName("토큰 발급 — Basic Auth 헤더 형식")
    fun token_issue_basic_auth() {
        val basic = Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
        mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/x-www-form-urlencoded")
                .header("Authorization", "Basic $basic")
                .param("grant_type", "client_credentials")
                .param("scope", "sf.write")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
    }

    @Test
    @DisplayName("토큰 발급 거부 — 잘못된 client_id (401 invalid_client)")
    fun token_invalid_client() {
        mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_id", "wrong-client")
                .param("client_secret", "wrong-secret")
                .param("scope", "sf.write")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("토큰 발급 거부 — 잘못된 grant_type (400 unsupported_grant_type)")
    fun token_unsupported_grant() {
        mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "password")
                .param("client_id", CLIENT_ID)
                .param("client_secret", CLIENT_SECRET)
                .param("scope", "sf.write")
        )
            .andExpect(status().isBadRequest)
    }
}
