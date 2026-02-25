package com.otoki.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.io.File

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("OpenAPI Spec 자동 생성")
class OpenApiSpecGeneratorTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Test
    @DisplayName("OpenAPI spec JSON 파일 생성")
    fun generateOpenApiSpec() {
        // Given
        val outputFile = File(System.getProperty("user.dir"), "openapi.json")

        // When
        val response = restTemplate.getForEntity("/v3/api-docs", String::class.java)

        // Then — HTTP 200 응답
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val body = response.body!!
        val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val jsonNode = objectMapper.readTree(body)

        // Then — JSON에 openapi 키 존재
        assertThat(jsonNode.has("openapi")).isTrue()
        assertThat(jsonNode.has("paths")).isTrue()

        // Pretty-print 후 파일 저장
        val prettyJson = objectMapper.writeValueAsString(jsonNode)
        outputFile.writeText(prettyJson)

        // Then — 파일 생성 확인
        assertThat(outputFile.exists()).isTrue()
        assertThat(outputFile.length()).isGreaterThan(0)

        println("OpenAPI spec generated: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
    }
}
