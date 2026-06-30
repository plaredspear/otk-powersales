package com.otoki.powersales.external.sap.inbound.toggle

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper

@DisplayName("SapInboundToggleInterceptor 테스트")
class SapInboundToggleInterceptorTest {

    private val toggleStore: SapInboundToggleStore = mockk()
    private val auditService: SapInboundAuditService = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()

    private val interceptor = SapInboundToggleInterceptor(toggleStore, auditService, objectMapper)

    private fun request(uri: String = "/api/v1/sap/account") =
        MockHttpServletRequest("POST", uri).apply { remoteAddr = "10.0.0.1" }

    @Test
    @DisplayName("활성 endpoint - true 반환(통과) + audit/응답 미기록")
    fun enabledPassesThrough() {
        every { toggleStore.isEnabled("/api/v1/sap/account") } returns true
        val response = MockHttpServletResponse()

        val proceed = interceptor.preHandle(request(), response, Any())

        assertThat(proceed).isTrue()
        assertThat(response.contentAsString).isEmpty()
        verify(exactly = 0) { auditService.record(any()) }
    }

    @Test
    @DisplayName("비활성 endpoint - false 반환(차단) + SapResultWrapper.ok 응답")
    fun disabledReturnsOkAndBlocks() {
        every { toggleStore.isEnabled("/api/v1/sap/account") } returns false
        val response = MockHttpServletResponse()

        val proceed = interceptor.preHandle(request(), response, Any())

        assertThat(proceed).isFalse()
        assertThat(response.status).isEqualTo(200)
        assertThat(response.contentType).contains("application/json")
        assertThat(response.contentAsString).contains("\"RESULT_CODE\":\"200\"")
        assertThat(response.contentAsString).contains("\"RESULT_MSG\":\"OK\"")
    }

    @Test
    @DisplayName("비활성 endpoint - REQUEST_SKIPPED audit 기록")
    fun disabledRecordsSkippedAudit() {
        every { toggleStore.isEnabled("/api/v1/sap/account") } returns false
        val auditSlot = slot<SapInboundAudit>()
        every { auditService.record(capture(auditSlot)) } answers { auditSlot.captured }

        interceptor.preHandle(request(), MockHttpServletResponse(), Any())

        assertThat(auditSlot.captured.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_SKIPPED)
        assertThat(auditSlot.captured.endpoint).isEqualTo("/api/v1/sap/account")
        assertThat(auditSlot.captured.httpMethod).isEqualTo("POST")
        assertThat(auditSlot.captured.clientIp).isEqualTo("10.0.0.1")
    }

    @Test
    @DisplayName("audit 기록 실패해도 정상 응답은 반환된다 (best-effort)")
    fun auditFailureDoesNotBlockResponse() {
        every { toggleStore.isEnabled(any()) } returns false
        every { auditService.record(any()) } throws RuntimeException("audit down")
        val response = MockHttpServletResponse()

        val proceed = interceptor.preHandle(request(), response, Any())

        assertThat(proceed).isFalse()
        assertThat(response.contentAsString).contains("\"RESULT_CODE\":\"200\"")
    }
}
