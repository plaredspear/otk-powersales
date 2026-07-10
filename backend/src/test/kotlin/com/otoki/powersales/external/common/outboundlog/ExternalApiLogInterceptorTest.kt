package com.otoki.powersales.external.common.outboundlog

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import com.otoki.powersales.external.sf.outbound.SfResponseCountExtractor
import com.otoki.powersales.external.sf.outbound.SfResponseSuccessExtractor
import tools.jackson.databind.json.JsonMapper
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

@DisplayName("ExternalApiLogInterceptor 테스트")
class ExternalApiLogInterceptorTest {

    private lateinit var logService: ExternalApiLogService

    @BeforeEach
    fun setUp() {
        logService = mockk(relaxed = true)
    }

    /** captureBody 설정에 맞춰 인터셉터를 단 RestClient + 바인딩된 MockRestServiceServer 를 만든다. */
    private fun buildClient(captureBody: Boolean): Pair<RestClient, MockRestServiceServer> {
        val builder = RestClient.builder()
            .baseUrl("http://api-mock")
            .requestInterceptor(ExternalApiLogInterceptor(ExternalApiTarget.SAP, logService, captureBody))
        val server = MockRestServiceServer.bindTo(builder).build()
        return builder.build() to server
    }

    /** count resolver 를 주입한 인터셉터를 단 RestClient + MockRestServiceServer 를 만든다. */
    private fun buildClientWithCountResolver(
        resolver: (String?) -> Int?,
    ): Pair<RestClient, MockRestServiceServer> {
        val builder = RestClient.builder()
            .baseUrl("http://api-mock")
            .requestInterceptor(
                ExternalApiLogInterceptor(
                    target = ExternalApiTarget.SF,
                    logService = logService,
                    captureBody = false,
                    responseCountResolver = resolver,
                )
            )
        val server = MockRestServiceServer.bindTo(builder).build()
        return builder.build() to server
    }

    @Test
    @DisplayName("2xx 응답 — success=true 로 1건 적재 + 본문은 그대로 다운스트림에 전달")
    fun success() {
        val (restClient, server) = buildClient(captureBody = false)
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
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        val body = restClient.post()
            .uri("/SD03040")
            .body(mapOf("k" to "v"))
            .retrieve()
            .body(String::class.java)

        // 인터셉터가 본문 스트림을 소비하지 않아 다운스트림이 정상 파싱
        assertThat(body).isEqualTo("""{"resultCode":"S"}""")

        verify(exactly = 1) {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
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
        val (restClient, server) = buildClient(captureBody = false)
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
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        assertThatThrownBy {
            restClient.get().uri("/x").retrieve().body(String::class.java)
        }.isInstanceOf(Exception::class.java)

        assertThat(statusSlot.captured).isEqualTo(500)
        assertThat(successSlot.captured).isFalse()
    }

    @Test
    @DisplayName("4xx(401) 에러 응답에도 호출 이력 1건이 success=false 로 적재된다")
    fun clientError401IsRecorded() {
        val (restClient, server) = buildClient(captureBody = false)
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("Unauthorized"))

        val statusSlot = slot<Int?>()
        val successSlot = slot<Boolean>()
        val errorDetailSlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = captureNullable(statusSlot),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = captureNullable(errorDetailSlot),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        assertThatThrownBy {
            restClient.post().uri("/SD03040").body(mapOf("k" to "v")).retrieve().body(String::class.java)
        }.isInstanceOf(Exception::class.java)

        verify(exactly = 1) {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        assertThat(statusSlot.captured).isEqualTo(401)
        assertThat(successSlot.captured).isFalse()
        assertThat(errorDetailSlot.captured).isEqualTo("HTTP 401")
    }

    @Test
    @DisplayName("captureBody=true — 성공 응답의 요청/응답 본문이 적재되고 다운스트림도 정상 파싱")
    fun captureBodyOnSuccess() {
        val (restClient, server) = buildClient(captureBody = true)
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andRespond(withSuccess("""{"resultCode":"S"}""", MediaType.APPLICATION_JSON))

        val requestBodySlot = slot<String?>()
        val responseBodySlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = any(),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = captureNullable(requestBodySlot),
                responseBody = captureNullable(responseBodySlot),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        val body = restClient.post()
            .uri("/SD03040")
            .body(mapOf("k" to "v"))
            .retrieve()
            .body(String::class.java)

        // 본문을 buffering 으로 읽은 뒤에도 다운스트림이 동일 본문을 정상 수신
        assertThat(body).isEqualTo("""{"resultCode":"S"}""")
        assertThat(requestBodySlot.captured).isEqualTo("""{"k":"v"}""")
        assertThat(responseBodySlot.captured).isEqualTo("""{"resultCode":"S"}""")
    }

    @Test
    @DisplayName("captureBody=true — 401 에러 응답 본문도 적재된다")
    fun captureBodyOnError() {
        val (restClient, server) = buildClient(captureBody = true)
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("SAP auth failed"))

        val responseBodySlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = any(),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = captureNullable(responseBodySlot),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        assertThatThrownBy {
            restClient.post().uri("/SD03040").body(mapOf("k" to "v")).retrieve().body(String::class.java)
        }.isInstanceOf(Exception::class.java)

        assertThat(responseBodySlot.captured).isEqualTo("SAP auth failed")
    }

    @Test
    @DisplayName("captureBody=false — 본문은 적재하지 않는다 (request/response body null)")
    fun noBodyCaptureWhenDisabled() {
        val (restClient, server) = buildClient(captureBody = false)
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andRespond(withSuccess("""{"resultCode":"S"}""", MediaType.APPLICATION_JSON))

        val requestBodySlot = slot<String?>()
        val responseBodySlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = any(),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = captureNullable(requestBodySlot),
                responseBody = captureNullable(responseBodySlot),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        restClient.post().uri("/SD03040").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        assertThat(requestBodySlot.captured).isNull()
        assertThat(responseBodySlot.captured).isNull()
    }

    @Test
    @DisplayName("로그 적재 실패는 실제 호출을 막지 않음 (best-effort)")
    fun loggingFailureDoesNotBreakCall() {
        val (restClient, server) = buildClient(captureBody = false)
        every {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("DB down")

        server.expect(ExpectedCount.once(), requestTo("http://api-mock/ok"))
            .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN))

        val body = restClient.get().uri("/ok").retrieve().body(String::class.java)

        assertThat(body).isEqualTo("OK")
    }

    @Test
    @DisplayName("count resolver 주입 — 응답 본문에서 산출한 건수가 responseCount 로 적재된다")
    fun responseCountRecordedWhenResolverInjected() {
        // resolver: 실제 SF 추출기와 동일 규칙 (data 배열 크기)
        val (restClient, server) = buildClientWithCountResolver(sfCountResolver())
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/IF_x"))
            .andRespond(withSuccess("""{"data":[{"a":1},{"a":2},{"a":3}]}""", MediaType.APPLICATION_JSON))

        val responseCountSlot = slot<Int?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = any(),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = captureNullable(responseCountSlot)
            )
        } returns mockk<ExternalApiLog>()

        val body = restClient.post().uri("/IF_x").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        // resolver 가 data 배열 크기 3 을 산출 + 다운스트림도 동일 본문 정상 수신
        assertThat(body).isEqualTo("""{"data":[{"a":1},{"a":2},{"a":3}]}""")
        assertThat(responseCountSlot.captured).isEqualTo(3)
    }

    @Test
    @DisplayName("count resolver 미주입 — responseCount 는 null 로 적재된다")
    fun responseCountNullWhenNoResolver() {
        val (restClient, server) = buildClient(captureBody = false)
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/SD03040"))
            .andRespond(withSuccess("""{"data":[{"a":1}]}""", MediaType.APPLICATION_JSON))

        val responseCountSlot = slot<Int?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = any(),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = captureNullable(responseCountSlot)
            )
        } returns mockk<ExternalApiLog>()

        restClient.post().uri("/SD03040").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        assertThat(responseCountSlot.captured).isNull()
    }

    /** success resolver 를 주입한 인터셉터를 단 RestClient + MockRestServiceServer 를 만든다. */
    private fun buildClientWithSuccessResolver(
        resolver: (Int?, String?) -> OutboundSuccessVerdict?,
    ): Pair<RestClient, MockRestServiceServer> {
        val builder = RestClient.builder()
            .baseUrl("http://api-mock")
            .requestInterceptor(
                ExternalApiLogInterceptor(
                    target = ExternalApiTarget.SF,
                    logService = logService,
                    captureBody = false,
                    responseSuccessResolver = resolver,
                )
            )
        val server = MockRestServiceServer.bindTo(builder).build()
        return builder.build() to server
    }

    @Test
    @DisplayName("success resolver 주입 — HTTP 200 이어도 body 판정이 실패면 success=false + errorDetail 에 사유 적재")
    fun successResolverOverridesHttp200ToFailure() {
        val (restClient, server) = buildClientWithSuccessResolver(sfSuccessResolver())
        // HTTP 200 이지만 SF RESULT_CODE 가 성공(200) 이 아님 → 도메인 실패
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/ClaimRegist"))
            .andRespond(withSuccess("""{"RESULT_CODE":"500","RESULT_MSG":"필수값 누락"}""", MediaType.APPLICATION_JSON))

        val statusSlot = slot<Int?>()
        val successSlot = slot<Boolean>()
        val errorDetailSlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = captureNullable(statusSlot),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = captureNullable(errorDetailSlot),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        restClient.post().uri("/ClaimRegist").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        assertThat(statusSlot.captured).isEqualTo(200)
        assertThat(successSlot.captured).isFalse()
        assertThat(errorDetailSlot.captured).contains("RESULT_CODE=500").contains("필수값 누락")
    }

    @Test
    @DisplayName("success resolver 주입 — body 가 성공(RESULT_CODE=200) 이면 success=true")
    fun successResolverKeepsSuccessWhenResultCodeOk() {
        val (restClient, server) = buildClientWithSuccessResolver(sfSuccessResolver())
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/ClaimRegist"))
            .andRespond(withSuccess("""{"RESULT_CODE":"200","RESULT_MSG":"성공"}""", MediaType.APPLICATION_JSON))

        val successSlot = slot<Boolean>()
        val errorDetailSlot = slot<String?>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = captureNullable(errorDetailSlot),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        restClient.post().uri("/ClaimRegist").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        assertThat(successSlot.captured).isTrue()
        assertThat(errorDetailSlot.captured).isNull()
    }

    @Test
    @DisplayName("success resolver 가 null(판정 불가) 반환 — HTTP status 판정을 그대로 유지")
    fun successResolverFallsBackToHttpWhenNull() {
        val (restClient, server) = buildClientWithSuccessResolver(sfSuccessResolver())
        // RESULT_CODE 없는 fetch 목록 응답 → resolver null → HTTP 200 판정 유지
        server.expect(ExpectedCount.once(), requestTo("http://api-mock/list"))
            .andRespond(withSuccess("""{"Result":[{"a":1}]}""", MediaType.APPLICATION_JSON))

        val successSlot = slot<Boolean>()
        every {
            logService.log(
                targetSystem = any(),
                endpointKey = any(),
                httpMethod = any(),
                uri = any(),
                httpStatus = any(),
                success = capture(successSlot),
                durationMs = any(),
                errorDetail = any(),
                requestedAt = any(),
                completedAt = any(),
                requestBody = any(),
                responseBody = any(),
                responseCount = any()            )
        } returns mockk<ExternalApiLog>()

        restClient.post().uri("/list").body(mapOf("k" to "v")).retrieve().body(String::class.java)

        assertThat(successSlot.captured).isTrue()
    }

    /** 테스트용 count resolver — 실제 [SfResponseCountExtractor] 로 data 배열 크기를 센다. */
    private fun sfCountResolver(): (String?) -> Int? {
        val extractor = SfResponseCountExtractor(JsonMapper.builder().build())
        return extractor::extract
    }

    /** 테스트용 success resolver — 실제 [SfResponseSuccessExtractor] 로 RESULT_CODE 를 판정한다. */
    private fun sfSuccessResolver(): (Int?, String?) -> OutboundSuccessVerdict? {
        val extractor = SfResponseSuccessExtractor(JsonMapper.builder().build())
        return extractor::resolve
    }
}
