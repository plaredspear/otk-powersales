package com.otoki.powersales.external.common.outboundlog

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

@DisplayName("ExternalApiLogInterceptor 테스트")
class ExternalApiLogInterceptorTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var restClient: RestClient
    private lateinit var logService: ExternalApiLogService

    @BeforeEach
    fun setUp() {
        logService = mockk(relaxed = true)
        val builder = RestClient.builder()
            .baseUrl("http://api-mock")
            .requestInterceptor(ExternalApiLogInterceptor(ExternalApiTarget.SAP, logService))
        server = MockRestServiceServer.bindTo(builder).build()
        restClient = builder.build()
    }

    @Test
    @DisplayName("2xx 응답 — success=true 로 1건 적재 + 본문은 그대로 다운스트림에 전달")
    fun success() {
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"resultCode":"S"}""", MediaType.APPLICATION_JSON))

        val targetSlot = slot<String>()
        val endpointKeySlot = slot<String?>()
        val methodSlot = slot<String>()
        val uriSlot = slot<String>()
        val statusSlot = slot<Int?>()
        val successSlot = slot<Boolean>()
        every {
            logService.log(
                targetSystem = capture(targetSlot),
                endpointKey = captureNullable(endpointKeySlot),
                httpMethod = capture(methodSlot),
                uri = capture(uriSlot),
                httpStatus = captureNullable(statusSlot),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any()
            )
        } returns mockk<ExternalApiLog>()

        val body = restClient.post()
            .uri("/SD03040")
            .body(mapOf("k" to "v"))
            .retrieve()
            .body(String::class.java)

        // 인터셉터가 본문 스트림을 소비하지 않아 다운스트림이 정상 파싱
        assertThat(body).isEqualTo("""{"resultCode":"S"}""")

        verify(exactly = 1) { logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(targetSlot.captured).isEqualTo(ExternalApiTarget.SAP)
        assertThat(methodSlot.captured).isEqualTo("POST")
        assertThat(uriSlot.captured).isEqualTo("http://api-mock/SD03040")
        assertThat(statusSlot.captured).isEqualTo(200)
        assertThat(successSlot.captured).isTrue()
        // resolver 가 SAP interfaceId SD03040 → loan-inquiry 탭 key 로 분류
        assertThat(endpointKeySlot.captured).isEqualTo("loan-inquiry")
    }

    @Test
    @DisplayName("5xx 응답 — success=false + httpStatus=500 으로 적재")
    fun serverError() {
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/x"))
            .andRespond(withServerError())

        val statusSlot = slot<Int?>()
        val successSlot = slot<Boolean>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = captureNullable(statusSlot),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any()
            )
        } returns mockk<ExternalApiLog>()

        assertThatThrownBy {
            restClient.get().uri("/x").retrieve().body(String::class.java)
        }.isInstanceOf(Exception::class.java)

        assertThat(statusSlot.captured).isEqualTo(500)
        assertThat(successSlot.captured).isFalse()
    }

    @Test
    @DisplayName("로그 적재 실패는 실제 호출을 막지 않음 (best-effort)")
    fun loggingFailureDoesNotBreakCall() {
        every {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("DB down")

        server.expect(ExpectedCount.once(), requestTo("http://api-mock/ok"))
            .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN))

        val body = restClient.get().uri("/ok").retrieve().body(String::class.java)

        assertThat(body).isEqualTo("OK")
    }
}
