package com.otoki.powersales.sap.outbound.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.otoki.powersales.sap.outbound.config.SapOutboundProperties
import com.otoki.powersales.sap.outbound.dto.SapOutboundRequest
import com.otoki.powersales.sap.outbound.dto.SapOutboundResponse
import com.otoki.powersales.sap.outbound.exception.SapOutboundException
import com.otoki.powersales.sap.outbound.service.SapOutboundLogService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.SocketTimeoutException

@ExtendWith(MockitoExtension::class)
@DisplayName("SapOutboundClientImpl 테스트")
class SapOutboundClientImplTest {

    @Mock
    private lateinit var logService: SapOutboundLogService

    private lateinit var restClientBuilder: RestClient.Builder
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var client: SapOutboundClientImpl
    private lateinit var properties: SapOutboundProperties
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    private val baseUrl = "http://sap-mock.local"
    private val path = "/api/sap/SD03300"
    private val interfaceId = "SD03300"

    @BeforeEach
    fun setUp() {
        properties = SapOutboundProperties(
            baseUrl = baseUrl,
            username = "u",
            password = "p",
            connectTimeoutMs = 1000,
            readTimeoutMs = 1000,
            retry = SapOutboundProperties.Retry(maxAttempts = 3, delayMs = 0)
        )
        val jacksonConverter = MappingJackson2HttpMessageConverter(objectMapper)
        restClientBuilder = RestClient.builder()
            .baseUrl(baseUrl)
            .messageConverters { converters ->
                converters.removeIf { it is MappingJackson2HttpMessageConverter }
                converters.add(0, jacksonConverter)
            }
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        val restClient = restClientBuilder.build()
        client = SapOutboundClientImpl(restClient, properties, logService)
    }

    private fun successJson() = objectMapper.writeValueAsString(
        mapOf("result_code" to "200", "result_msg" to "SUCCESS")
    )

    private fun sampleRequest(items: List<Map<String, Any>> = listOf(mapOf("itemNo" to "001"))) =
        SapOutboundRequest(interfaceId = interfaceId, reqItemList = items)

    @Nested
    @DisplayName("send - 정상 흐름")
    inner class SuccessCase {

        @Test
        @DisplayName("정상 전송 - 200 응답 -> resultCode 200, attemptCount=1 로그 저장")
        fun success_firstAttempt() {
            mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

            val response = client.send(path, sampleRequest())

            assertThat(response.resultCode).isEqualTo(SapOutboundResponse.SUCCESS_CODE)
            assertThat(response.resultMsg).isEqualTo("SUCCESS")
            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(1),
                httpStatus = eq(200),
                resultCode = eq("200"),
                resultMsg = eq("SUCCESS"),
                attemptCount = eq(1),
                durationMs = any(),
                errorDetail = isNull(),
                requestedAt = any(),
                completedAt = any()
            )
        }

        @Test
        @DisplayName("재시도 성공 - 1차 500 → 2차 200 -> attemptCount=2 로 로그 1건")
        fun success_afterRetry() {
            mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError())
            mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

            val response = client.send(path, sampleRequest())

            assertThat(response.resultCode).isEqualTo("200")
            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(1),
                httpStatus = eq(200),
                resultCode = eq("200"),
                resultMsg = eq("SUCCESS"),
                attemptCount = eq(2),
                durationMs = any(),
                errorDetail = isNull(),
                requestedAt = any(),
                completedAt = any()
            )
        }
    }

    @Nested
    @DisplayName("send - 실패 흐름")
    inner class FailureCase {

        @Test
        @DisplayName("최대 재시도 초과 - 3회 모두 500 -> SapOutboundException, attemptCount=3 로그")
        fun fail_retryExhausted() {
            repeat(3) {
                mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError())
            }

            assertThatThrownBy { client.send(path, sampleRequest()) }
                .isInstanceOf(SapOutboundException::class.java)

            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(1),
                httpStatus = eq(500),
                resultCode = isNull(),
                resultMsg = isNull(),
                attemptCount = eq(3),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any()
            )
        }

        @Test
        @DisplayName("4xx 응답 - 즉시 SapOutboundException, 재시도 없음, attemptCount=1")
        fun fail_clientErrorNoRetry() {
            mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad"))

            assertThatThrownBy { client.send(path, sampleRequest()) }
                .isInstanceOf(SapOutboundException::class.java)

            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(1),
                httpStatus = eq(400),
                resultCode = isNull(),
                resultMsg = isNull(),
                attemptCount = eq(1),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any()
            )
        }

        @Test
        @DisplayName("연결 실패(타임아웃) - 재시도 후 SapOutboundException, httpStatus=null")
        fun fail_networkError() {
            repeat(3) {
                mockServer.expect(ExpectedCount.once(), requestTo("$baseUrl$path"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond { throw SocketTimeoutException("read timeout") }
            }

            assertThatThrownBy { client.send(path, sampleRequest()) }
                .isInstanceOf(SapOutboundException::class.java)

            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(1),
                httpStatus = isNull(),
                resultCode = isNull(),
                resultMsg = isNull(),
                attemptCount = eq(3),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any()
            )
        }
    }

    @Nested
    @DisplayName("send - 부가 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("빈 reqItemList - 전송 없이 즉시 반환, 로그에 requestCount=0 / attemptCount=0")
        fun empty_request_short_circuit() {
            val response = client.send(
                path,
                SapOutboundRequest(interfaceId = interfaceId, reqItemList = emptyList<String>())
            )

            assertThat(response.resultCode).isEqualTo("200")
            assertThat(response.resultMsg).isEqualTo("SKIPPED_EMPTY")
            mockServer.verify()
            verify(logService).log(
                interfaceId = eq(interfaceId),
                endpointPath = eq(path),
                requestCount = eq(0),
                httpStatus = isNull(),
                resultCode = eq("200"),
                resultMsg = eq("SKIPPED_EMPTY"),
                attemptCount = eq(0),
                durationMs = any(),
                errorDetail = isNull(),
                requestedAt = any(),
                completedAt = any()
            )
        }

        @Test
        @DisplayName("base-url 미설정 - IllegalStateException 즉시 발생, 로그 미저장")
        fun missing_baseUrl_throws_illegalState() {
            val emptyProps = SapOutboundProperties(baseUrl = "", username = "u", password = "p")
            val noBaseClient = SapOutboundClientImpl(restClientBuilder.build(), emptyProps, logService)

            assertThatThrownBy { noBaseClient.send(path, sampleRequest()) }
                .isInstanceOf(IllegalStateException::class.java)

            verifyNoInteractions(logService)
        }
    }

}
