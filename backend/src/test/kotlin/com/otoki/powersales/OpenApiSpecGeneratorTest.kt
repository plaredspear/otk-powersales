package com.otoki.powersales

import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.io.File

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@DisplayName("OpenAPI Spec 자동 생성")
class OpenApiSpecGeneratorTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Test
    @DisplayName("OpenAPI spec JSON 파일 생성 (통합 + 그룹별)")
    fun generateOpenApiSpec() {
        // GroupedOpenApi 빈에 대응하는 그룹별 endpoint 도 추가 생성한다 (OpenApiConfig).
        // 통합본은 `/v3/api-docs`, 그룹본은 `/v3/api-docs/<group>` 으로 노출되며
        // 각각 backend/openapi.json, backend/openapi-<group>.json 으로 저장된다.
        val groups = listOf(
            "" to "openapi.json",
            "admin" to "openapi-admin.json",
            "sap" to "openapi-sap.json",
            "mobile" to "openapi-mobile.json"
        )
        val objectMapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
        val baseUrl = "/v3/api-docs"

        for ((group, fileName) in groups) {
            val url = if (group.isEmpty()) baseUrl else "$baseUrl/$group"
            val response = restTemplate.getForEntity(url, String::class.java)

            assertThat(response.statusCode)
                .withFailMessage { "OpenAPI 응답 실패: $url -> ${response.statusCode}" }
                .isEqualTo(HttpStatus.OK)

            val body = response.body!!
            val jsonNode = objectMapper.readTree(body)
            assertThat(jsonNode.has("openapi"))
                .withFailMessage { "openapi 키 누락: $url" }
                .isTrue()
            assertThat(jsonNode.has("paths"))
                .withFailMessage { "paths 키 누락: $url" }
                .isTrue()

            val prettyJson = objectMapper.writeValueAsString(jsonNode)
            val outputFile = File(System.getProperty("user.dir"), fileName)
            outputFile.writeText(prettyJson)

            assertThat(outputFile.exists()).isTrue()
            assertThat(outputFile.length()).isGreaterThan(0)
            println("OpenAPI spec generated: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        }
    }
}
