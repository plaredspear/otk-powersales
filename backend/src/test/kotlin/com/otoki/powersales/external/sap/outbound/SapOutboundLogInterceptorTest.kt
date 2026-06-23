package com.otoki.powersales.external.sap.outbound

import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

/**
 * 모든 SAP outbound 호출이 [SapOutboundLogService] 로 일괄 적재되는지 검증.
 *
 * RestClient builder 에 [SapOutboundLogInterceptor] 를 달아 MockRestServiceServer 에 bind 하면
 * 실제 송신 시 인터셉터가 호출되어 `sap_outbound_log` 적재(여기선 mock verify)가 일어난다.
 */
@DisplayName("SapOutboundLogInterceptor 테스트")
class SapOutboundLogInterceptorTest {

    private val objectMapper = ObjectMapper()
    private lateinit var logService: SapOutboundLogService
    private lateinit var server: MockRestServiceServer
    private lateinit var client: RestClient

    @BeforeEach
    fun setUp() {
        logService = mockk(relaxed = true)
        every {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { mockk(relaxed = true) }

        val builder = RestClient.builder()
            .baseUrl("http://sap-mock")
            .requestInterceptor(SapOutboundLogInterceptor(logService, objectMapper))
        server = MockRestServiceServer.bindTo(builder).build()
        client = builder.build()
    }

    @Test
    @DisplayName("단건 동기 호출 — interface_id/request_count=1/result_code=SUCCESS 적재")
    fun singleCallSuccess() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03052"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"resultCode":"S","resutlMsg":"OK"}""", MediaType.APPLICATION_JSON))

        client.post().uri("/SD03052")
            .body(mapOf("request" to mapOf("RequestNumber" to "ORD-1")))
            .retrieve().toEntity(String::class.java)

        val ifId = slot<String>()
        val reqCount = slot<Int>()
        val resultCode = slot<String?>()
        verify(exactly = 1) {
            logService.log(
                interfaceId = capture(ifId),
                endpointPath = "/SD03052",
                requestCount = capture(reqCount),
                httpStatus = 200,
                resultCode = captureNullable(resultCode),
                resultMsg = "OK",
                attemptCount = 1,
                durationMs = any(),
                errorDetail = null,
                requestedAt = any(),
                completedAt = any(),
            )
        }
        assertThat(ifId.captured).isEqualTo("SD03052")
        assertThat(reqCount.captured).isEqualTo(1)
        assertThat(resultCode.captured).isEqualTo("SUCCESS")
    }

    @Test
    @DisplayName("배열 페이로드 — request_count 가 배열 크기")
    fun arrayPayloadRequestCount() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03131"))
            .andRespond(withSuccess("""{"resultCode":"S"}""", MediaType.APPLICATION_JSON))

        client.post().uri("/SD03131")
            .body(mapOf("request" to listOf(mapOf("a" to "1"), mapOf("a" to "2"), mapOf("a" to "3"))))
            .retrieve().toEntity(String::class.java)

        val reqCount = slot<Int>()
        verify { logService.log(any(), any(), capture(reqCount), any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(reqCount.captured).isEqualTo(3)
    }

    @Test
    @DisplayName("HTTP 5xx — http_status 보존 + result_code=FAIL 적재")
    fun httpErrorRecorded() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03050"))
            .andRespond(withServerError())

        // 인터셉터는 5xx 응답을 정상 수신해 http_status=500 으로 기록한다.
        // retrieve() 의 5xx→예외 변환은 인터셉터 바깥에서 일어나므로 runCatching 으로 흡수.
        runCatching {
            client.post().uri("/SD03050")
                .body(mapOf("request" to mapOf("x" to "1")))
                .retrieve().toEntity(String::class.java)
        }

        val status = slot<Int?>()
        val resultCode = slot<String?>()
        verify(exactly = 1) {
            logService.log(
                interfaceId = "SD03050",
                endpointPath = "/SD03050",
                requestCount = 1,
                httpStatus = captureNullable(status),
                resultCode = captureNullable(resultCode),
                resultMsg = any(),
                attemptCount = 1,
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
            )
        }
        assertThat(status.captured).isEqualTo(500)
        assertThat(resultCode.captured).isEqualTo("FAIL")
    }

    @Test
    @DisplayName("HTML 응답 — result_code=INVALID_RESPONSE 적재")
    fun htmlResponseInvalid() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03052"))
            .andRespond(withSuccess("<html><body>error</body></html>", MediaType.TEXT_HTML))

        client.post().uri("/SD03052")
            .body(mapOf("request" to mapOf("x" to "1")))
            .retrieve().toEntity(String::class.java)

        verify(exactly = 1) {
            logService.log(
                interfaceId = "SD03052",
                endpointPath = "/SD03052",
                requestCount = 1,
                httpStatus = 200,
                resultCode = "INVALID_RESPONSE",
                resultMsg = "HTML_RESPONSE_DETECTED",
                attemptCount = 1,
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
            )
        }
    }
}
