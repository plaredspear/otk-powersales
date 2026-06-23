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
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

/**
 * 모든 SAP outbound 호출 결과가 [SapOutboundLogService] 로 적재되는지 검증.
 *
 * 범용 인터셉터가 호출하는 sink 를 직접 테스트한다 (인터셉터-RestClient 연동은
 * SapOutboundRestClientConfigTest 의 통합 케이스가 별도 검증).
 */
@DisplayName("SapOutboundResponseSink 테스트")
class SapOutboundResponseSinkTest {

    private val objectMapper = ObjectMapper()
    private lateinit var logService: SapOutboundLogService
    private lateinit var sink: SapOutboundResponseSink

    private val requestedAt = LocalDateTime.of(2026, 6, 23, 9, 0, 0)

    @BeforeEach
    fun setUp() {
        logService = mockk(relaxed = true)
        every {
            logService.log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { mockk(relaxed = true) }
        sink = SapOutboundResponseSink(logService, objectMapper)
    }

    @Test
    @DisplayName("단건 성공 — interface_id/request_count=1/result_code=SUCCESS")
    fun singleSuccess() {
        sink.accept(
            uri = "http://sap-mock/SD03052",
            requestBody = """{"request":{"RequestNumber":"ORD-1"}}""",
            httpStatus = 200,
            responseBody = """{"resultCode":"S","resutlMsg":"OK"}""",
            requestedAt = requestedAt,
            durationMs = 12,
            networkError = false,
        )

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
                durationMs = 12,
                errorDetail = null,
                requestedAt = requestedAt,
                completedAt = any(),
            )
        }
        assertThat(ifId.captured).isEqualTo("SD03052")
        assertThat(reqCount.captured).isEqualTo(1)
        assertThat(resultCode.captured).isEqualTo("SUCCESS")
    }

    @Test
    @DisplayName("배열 페이로드 — request_count 가 배열 크기")
    fun arrayPayload() {
        sink.accept(
            uri = "http://sap-mock/SD03131",
            requestBody = """{"request":[{"a":"1"},{"a":"2"},{"a":"3"}]}""",
            httpStatus = 200,
            responseBody = """{"resultCode":"S"}""",
            requestedAt = requestedAt,
            durationMs = 5,
            networkError = false,
        )

        val reqCount = slot<Int>()
        verify { logService.log(any(), any(), capture(reqCount), any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(reqCount.captured).isEqualTo(3)
    }

    @Test
    @DisplayName("네트워크 실패 — http_status=null + result_code=FAIL")
    fun networkError() {
        sink.accept(
            uri = "http://sap-mock/SD03050",
            requestBody = """{"request":{"x":"1"}}""",
            httpStatus = null,
            responseBody = null,
            requestedAt = requestedAt,
            durationMs = 5000,
            networkError = true,
        )

        verify(exactly = 1) {
            logService.log(
                interfaceId = "SD03050",
                endpointPath = "/SD03050",
                requestCount = 1,
                httpStatus = null,
                resultCode = "FAIL",
                resultMsg = "NETWORK_ERROR",
                attemptCount = 1,
                durationMs = 5000,
                errorDetail = null,
                requestedAt = requestedAt,
                completedAt = any(),
            )
        }
    }

    @Test
    @DisplayName("HTML 응답 — result_code=INVALID_RESPONSE + error_detail 보존")
    fun htmlResponse() {
        sink.accept(
            uri = "http://sap-mock/SD03052",
            requestBody = """{"request":{"x":"1"}}""",
            httpStatus = 200,
            responseBody = "<html><body>error</body></html>",
            requestedAt = requestedAt,
            durationMs = 8,
            networkError = false,
        )

        val errorDetail = slot<String?>()
        verify(exactly = 1) {
            logService.log(
                interfaceId = "SD03052",
                endpointPath = "/SD03052",
                requestCount = 1,
                httpStatus = 200,
                resultCode = "INVALID_RESPONSE",
                resultMsg = "HTML_RESPONSE_DETECTED",
                attemptCount = 1,
                durationMs = 8,
                errorDetail = captureNullable(errorDetail),
                requestedAt = requestedAt,
                completedAt = any(),
            )
        }
        assertThat(errorDetail.captured).contains("<html>")
    }

    @Test
    @DisplayName("SAP 업무 실패(resultCode!=S) — result_code=FAIL + 메시지 보존")
    fun businessFail() {
        sink.accept(
            uri = "http://sap-mock/SD03050",
            requestBody = """{"request":{"x":"1"}}""",
            httpStatus = 200,
            responseBody = """{"resultCode":"E","resutlMsg":"재고 부족"}""",
            requestedAt = requestedAt,
            durationMs = 30,
            networkError = false,
        )

        verify(exactly = 1) {
            logService.log(
                interfaceId = "SD03050",
                endpointPath = "/SD03050",
                requestCount = 1,
                httpStatus = 200,
                resultCode = "FAIL",
                resultMsg = "재고 부족",
                attemptCount = 1,
                durationMs = 30,
                errorDetail = null,
                requestedAt = requestedAt,
                completedAt = any(),
            )
        }
    }
}
