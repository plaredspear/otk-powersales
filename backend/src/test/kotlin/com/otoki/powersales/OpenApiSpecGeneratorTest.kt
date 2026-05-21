package com.otoki.powersales

import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
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

            // git diff 노이즈 제거: (1) servers[].url 의 RANDOM_PORT 정규화, (2) ObjectNode 키 재귀 정렬.
            // RANDOM_PORT 와 JVM 리플렉션 순서 비결정성으로 인해 재생성 때마다 키 순서/포트가 흔들리는 문제를 차단한다.
            normalizeServerUrls(jsonNode)
            val canonical = sortKeysRecursively(jsonNode, objectMapper)

            val prettyJson = objectMapper.writeValueAsString(canonical)
            val outputFile = File(System.getProperty("user.dir"), fileName)
            outputFile.writeText(prettyJson)

            assertThat(outputFile.exists()).isTrue()
            assertThat(outputFile.length()).isGreaterThan(0)
            println("OpenAPI spec generated: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        }
    }

    private fun normalizeServerUrls(root: JsonNode) {
        val servers = root.get("servers") as? ArrayNode ?: return
        for (server in servers) {
            val obj = server as? ObjectNode ?: continue
            val url = obj.get("url")?.asString() ?: continue
            // http://localhost:<port> 형태의 RANDOM_PORT 만 정규화. 외부 server URL 은 건드리지 않는다.
            val normalized = url.replace(Regex("^http://localhost:\\d+"), "http://localhost")
            obj.put("url", normalized)
        }
    }

    private fun sortKeysRecursively(node: JsonNode, mapper: JsonMapper): JsonNode {
        return when (node) {
            is ObjectNode -> {
                val sorted = mapper.createObjectNode()
                node.propertyNames().asSequence().sorted().forEach { key ->
                    sorted.set(key, sortKeysRecursively(node.get(key), mapper))
                }
                sorted
            }
            is ArrayNode -> {
                val arr = mapper.createArrayNode()
                node.forEach { element -> arr.add(sortKeysRecursively(element, mapper)) }
                arr
            }
            else -> node
        }
    }
}
