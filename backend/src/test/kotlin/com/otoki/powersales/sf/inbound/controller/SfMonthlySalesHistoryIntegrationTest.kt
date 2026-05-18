package com.otoki.powersales.sf.inbound.controller

import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.sf.auth.dto.TokenResponse
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/**
 * SF 인바운드 MonthlySalesHistory 종단 통합 테스트 (Spec #775).
 *
 * (1) /api/v1/sf/oauth/token 으로 token 발급
 * (2) /api/v1/sf/inbound/monthly-sales-history 호출
 * (3) 동일 도메인 service (MonthlySalesHistoryUpsertService) 가 RDS 적재
 * (4) 응답 RESULT_CODE=200 + entity 영속 확인
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["sf.auth.allowed-scopes[0]=sf.write"])
@DisplayName("SF 인바운드 MonthlySalesHistory 통합 테스트")
class SfMonthlySalesHistoryIntegrationTest {

    companion object {
        const val CLIENT_ID = "otoki-sf-msh-client"
        const val CLIENT_SECRET = "sf-msh-secret"
        private val SECRET_HASH: String = BCryptPasswordEncoder().encode(CLIENT_SECRET)!!

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("sf.auth.client-id") { CLIENT_ID }
            registry.add("sf.auth.client-secret-hash") { SECRET_HASH }
            registry.add("sf.auth.jwt-signing-key") { "sf-msh-integration-jwt-signing-key-with-256-bits-1234567890" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @MockitoBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private fun issueToken(): String {
        val result = mockMvc.perform(
            post("/api/v1/sf/oauth/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "client_credentials")
                .param("client_id", CLIENT_ID)
                .param("client_secret", CLIENT_SECRET)
                .param("scope", "sf.write")
        ).andExpect(status().isOk).andReturn()
        return objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java).accessToken
    }

    @Test
    @DisplayName("정상 적재 - RDS 에 row 영속 + RESULT_CODE=200")
    fun upsert_success() {
        val token = issueToken()
        val body = """
            {
              "reqItemList": [
                {
                  "SAPAccountCode": "9999001",
                  "SalesYearMonth": "202605",
                  "ABCClosingAmount1": "1000000",
                  "ABCClosingAmount2": "1200000",
                  "ABCClosingAmount3": "1500000",
                  "ShipClosingAmount": "5000000",
                  "TotalLedgerAmount": "10000000",
                  "rlsales": "0"
                }
              ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/sf/inbound/monthly-sales-history")
                .contentType("application/json")
                .header("Authorization", "Bearer $token")
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.RESULT_CODE").value("200"))
            .andExpect(jsonPath("$.RESULT_MSG").value("OK"))
            .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
            .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(0))

        val persisted = monthlySalesHistoryRepository.findByExternalkeyC("9999001202605")
        assertThat(persisted).isNotNull
        assertThat(persisted?.externalkeyC).isEqualTo("9999001202605")
        assertThat(persisted?.shipClosingAmount).isEqualTo(5_000_000.0)
    }

    @Test
    @DisplayName("재호출 - 동일 Externalkey 로 upsert 되어 1건만 보존")
    fun upsert_idempotent() {
        val token = issueToken()
        val body = """
            {
              "reqItemList": [
                {"SAPAccountCode":"9999002","SalesYearMonth":"202605","ShipClosingAmount":"1000"}
              ]
            }
        """.trimIndent()

        repeat(2) {
            mockMvc.perform(
                post("/api/v1/sf/inbound/monthly-sales-history")
                    .contentType("application/json")
                    .header("Authorization", "Bearer $token")
                    .content(body)
            ).andExpect(status().isOk)
        }

        val persisted = monthlySalesHistoryRepository.findByExternalkeyC("9999002202605")
        assertThat(persisted).isNotNull
    }

    @Test
    @DisplayName("토큰 없음 - 401")
    fun upsert_without_token() {
        mockMvc.perform(
            post("/api/v1/sf/inbound/monthly-sales-history")
                .contentType("application/json")
                .content("""{"reqItemList":[]}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("빈 reqItemList - 422 INVALID_PAYLOAD")
    fun upsert_empty_list() {
        val token = issueToken()
        mockMvc.perform(
            post("/api/v1/sf/inbound/monthly-sales-history")
                .contentType("application/json")
                .header("Authorization", "Bearer $token")
                .content("""{"reqItemList":[]}""")
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))
    }
}
