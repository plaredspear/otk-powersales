package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@DisplayName("SapInboundExceptionHandler 테스트")
class SapInboundExceptionHandlerTest {

    private val auditService: SapInboundAuditService = mock()
    private val handler = SapInboundExceptionHandler(auditService)

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest("POST", "/api/v1/sap/account")
        request.remoteAddr = "127.0.0.1"
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "sap-otoki",
                null,
                listOf(SimpleGrantedAuthority("SCOPE_sap.employee.write"))
            )
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("handleAccessDenied - 403 + INSUFFICIENT_SCOPE 응답")
    fun handleAccessDenied_responds403WithInsufficientScope() {
        val response = handler.handleAccessDenied(AccessDeniedException("Access is denied"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INSUFFICIENT_SCOPE)
        assertThat(response.body?.resultMsg).isEqualTo("권한 없음")
    }

    @Test
    @DisplayName("handleAccessDenied - REQUEST_REJECTED_SCOPE audit 1회 기록")
    fun handleAccessDenied_recordsScopeRejectionAudit() {
        handler.handleAccessDenied(AccessDeniedException("Access is denied"))

        val captor = argumentCaptor<SapInboundAudit>()
        verify(auditService).record(captor.capture())
        val audit = captor.firstValue

        assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_SCOPE)
        assertThat(audit.clientId).isEqualTo("sap-otoki")
        assertThat(audit.endpoint).isEqualTo("/api/v1/sap/account")
        assertThat(audit.httpMethod).isEqualTo("POST")
        assertThat(audit.scope).isEqualTo("sap.employee.write")
        assertThat(audit.reason).contains("Access is denied")
    }

    @Test
    @DisplayName("handleAccessDenied - RequestContextHolder 미설정 → audit 호출 안 함 (silent return)")
    fun handleAccessDenied_skipsAuditWhenRequestMissing() {
        RequestContextHolder.resetRequestAttributes()

        val response = handler.handleAccessDenied(AccessDeniedException("Access is denied"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        verify(auditService, never()).record(any())
    }

    @Test
    @DisplayName("handleAccessDenied - audit 적재 실패해도 403 응답은 유지 (runCatching 격리)")
    fun handleAccessDenied_isolatesAuditFailure() {
        doThrow(RuntimeException("DB down")).whenever(auditService).record(any())

        val response = handler.handleAccessDenied(AccessDeniedException("Access is denied"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INSUFFICIENT_SCOPE)
    }
}
