package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.controller.SapInboundExceptionHandler
import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import com.fasterxml.jackson.databind.ObjectMapper

@DisplayName("SapInboundExceptionHandler 테스트")
class SapInboundExceptionHandlerTest {

    private val auditService: SapInboundAuditService = mockk()
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

        val captor = slot<SapInboundAudit>()
        verify { auditService.record(capture(captor)) }
        val audit = captor.captured

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
        verify(exactly = 0) { auditService.record(any()) }
    }

    @Test
    @DisplayName("handleAccessDenied - audit 적재 실패해도 403 응답은 유지 (runCatching 격리)")
    fun handleAccessDenied_isolatesAuditFailure() {
        every { auditService.record(any()) } throws RuntimeException("DB down")

        val response = handler.handleAccessDenied(AccessDeniedException("Access is denied"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INSUFFICIENT_SCOPE)
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported - 415 + UNSUPPORTED_MEDIA_TYPE + Content-Type: application/json 강제")
    fun handleMediaTypeNotSupported_responds415AsJson() {
        val ex = HttpMediaTypeNotSupportedException(
            MediaType.TEXT_PLAIN,
            listOf(MediaType.APPLICATION_JSON)
        )

        val response = handler.handleMediaTypeNotSupported(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_UNSUPPORTED_MEDIA_TYPE)
        assertThat(response.body?.resultMsg).contains("application/json")
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
    }

    @Test
    @DisplayName("handleValidation - 400 + INVALID_PAYLOAD + REQUEST_REJECTED_PAYLOAD audit(실패 필드 경로 포함) 기록")
    fun handleValidation_responds400AndRecordsPayloadAudit() {
        every { auditService.record(any()) } returns mockk()
        val bindingResult = BeanPropertyBindingResult(Any(), "erpOrderRequest").apply {
            addError(FieldError("erpOrderRequest", "reqItemList", "reqItemList 필수"))
        }
        val ex = MethodArgumentNotValidException(mockk(relaxed = true), bindingResult)

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INVALID_PAYLOAD)
        assertThat(response.body?.resultMsg).isEqualTo("reqItemList 필수")

        val captor = slot<SapInboundAudit>()
        verify { auditService.record(capture(captor)) }
        val audit = captor.captured
        assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_PAYLOAD)
        assertThat(audit.endpoint).isEqualTo("/api/v1/sap/account")
        assertThat(audit.httpMethod).isEqualTo("POST")
        // 값 없이 실패 필드 경로만 기록
        assertThat(audit.reason).contains("reqItemList")
    }

    @Test
    @DisplayName("handleUnreadable - 400 + REQUEST_REJECTED_PAYLOAD audit(값 없이 구조 메타만) 기록")
    fun handleUnreadable_responds400AndRecordsStructuralMeta() {
        every { auditService.record(any()) } returns mockk()
        // reqItemList 를 배열이 아닌 문자열로 보내 타입 불일치(MismatchedInputException) 유발
        val mapper = ObjectMapper()
        val ex: HttpMessageNotReadableException = try {
            mapper.readValue(
                """{"reqItemList":"not-an-array"}""",
                TypeErpOrderLike::class.java
            )
            error("역직렬화가 실패해야 함")
        } catch (cause: Exception) {
            HttpMessageNotReadableException("parse failed", cause, mockk(relaxed = true))
        }

        val response = handler.handleUnreadable(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INVALID_PAYLOAD)
        assertThat(response.body?.resultMsg).isEqualTo("요청 본문이 올바르지 않습니다")

        val captor = slot<SapInboundAudit>()
        verify { auditService.record(capture(captor)) }
        val audit = captor.captured
        assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_PAYLOAD)
        // 구조 메타(실패 필드 path)는 남고, 업무 값("not-an-array")은 남지 않는다
        assertThat(audit.reason).contains("reqItemList")
        assertThat(audit.reason).doesNotContain("not-an-array")
    }

    @Test
    @DisplayName("handleUnreadable - audit 적재 실패해도 400 응답은 유지 (runCatching 격리)")
    fun handleUnreadable_isolatesAuditFailure() {
        every { auditService.record(any()) } throws RuntimeException("DB down")
        val ex = HttpMessageNotReadableException("broken", mockk(relaxed = true))

        val response = handler.handleUnreadable(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_INVALID_PAYLOAD)
    }

    private data class TypeErpOrderLike(val reqItemList: List<String>? = null)

    @Test
    @DisplayName("handleNotAcceptable - 406 + NOT_ACCEPTABLE + Content-Type: application/json 강제")
    fun handleNotAcceptable_responds406AsJson() {
        val response = handler.handleNotAcceptable()

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_ACCEPTABLE)
        assertThat(response.body?.resultCode).isEqualTo(SapResultWrapper.CODE_NOT_ACCEPTABLE)
        assertThat(response.body?.resultMsg).contains("application/json")
        // Accept 헤더가 잘못됐어도 응답은 항상 application/json (재귀적 직렬화 fail 차단)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
    }
}
