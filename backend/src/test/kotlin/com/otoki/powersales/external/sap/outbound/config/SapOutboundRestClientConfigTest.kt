package com.otoki.powersales.external.sap.outbound.config

import com.otoki.powersales.external.common.outboundlog.ExternalApiLogBodyCapture
import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 * `sapOutboundRestClient` 의 message converter 구성 회귀 테스트.
 *
 * 회귀 배경: `configureMessageConverters { withJsonConverter(...) }` 만 호출하고 `registerDefaults()`
 * 를 누락하면 기본 converter 체인이 비어, RestClient body writer 의 converter 후보 탐색에서
 * `mapOf` 단일 엔트리(`java.util.Collections$SingletonMap`) 페이로드(SD03040 등)가
 * `No HttpMessageConverter for java.util.Collections$SingletonMap and content type
 * "application/json"` 으로 직렬화 실패했다. 본 테스트는 config 가 실제로 빌드한 RestClient 의
 * converter 구성으로 SingletonMap 페이로드가 정상 직렬화되는지 검증한다.
 */
@DisplayName("SapOutboundRestClientConfig converter 구성 테스트")
class SapOutboundRestClientConfigTest {

    private fun newConfig(): SapOutboundRestClientConfig {
        val logService = mockk<ExternalApiLogService>(relaxed = true)
        every {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            mockk(relaxed = true)
        }
        val bodyCapture = mockk<ExternalApiLogBodyCapture>()
        every { bodyCapture.enabled } returns false
        return SapOutboundRestClientConfig(SapOutboundProperties(), logService, bodyCapture)
    }

    @Test
    @DisplayName("mapOf 단일 엔트리(SingletonMap) 페이로드가 No HttpMessageConverter 없이 직렬화된다")
    fun singletonMapPayloadSerializes() {
        val config = newConfig()
        val objectMapper = config.sapOutboundObjectMapper()
        // 실제 sender 가 RestClient 를 주입받는 것과 동일하게, config 가 구성한 converter 를
        // 그대로 적용한 builder 를 MockRestServiceServer 에 bind 한다.
        val builder = config.restClientBuilder(objectMapper).baseUrl("http://sap-mock")
        val server = MockRestServiceServer.bindTo(builder).build()

        // LoanInquirySender 와 동일 — 단일 엔트리 → Collections$SingletonMap 으로 떨어지는 페이로드
        val payload = mapOf("request" to mapOf("SAPAccountCode" to "1000900"))
        assertThat(payload.javaClass.name).isEqualTo("java.util.Collections\$SingletonMap")

        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03040"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"request":{"SAPAccountCode":"1000900"}}"""))
            .andRespond(withSuccess("""{"resultCode":"S"}""", MediaType.APPLICATION_JSON))

        val client = builder.build()
        assertThatCode {
            client.post()
                .uri("/SD03040")
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)
        }.doesNotThrowAnyException()

        server.verify()
    }
}
