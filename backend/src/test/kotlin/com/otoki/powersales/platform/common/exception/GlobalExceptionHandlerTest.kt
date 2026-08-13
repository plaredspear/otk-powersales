package com.otoki.powersales.platform.common.exception

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.platform.common.exception.GlobalExceptionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.context.request.ServletWebRequest

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    /**
     * 로그 레벨 검증용 appender — `BusinessException.serverFault` 판정이 실제 로그 레벨/스택 첨부로
     * 이어지는지 확인한다. 상태코드만 검증하면 "502 인데 ERROR 스택 150줄" 같은 노이즈 회귀를 못 잡는다.
     */
    private val logAppender = ListAppender<ILoggingEvent>()
    private val handlerLogger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java) as Logger

    @BeforeEach
    fun attachAppender() {
        logAppender.start()
        handlerLogger.addAppender(logAppender)
    }

    @AfterEach
    fun detachAppender() {
        handlerLogger.detachAppender(logAppender)
        logAppender.stop()
    }

    private fun singleLogEvent(): ILoggingEvent =
        logAppender.list.single { it.loggerName == GlobalExceptionHandler::class.java.name }

    @Test
    @DisplayName("handleMethodNotSupported - SAP 인바운드 path 는 SapResultWrapper 형식")
    fun handleMethodNotSupported_sapPathReturnsSapResultWrapper() {
        val ex = HttpRequestMethodNotSupportedException("GET", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/sap/employee"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
        val body = response.body as SapResultWrapper<*>
        assertThat(body.resultCode).isEqualTo(SapResultWrapper.CODE_METHOD_NOT_ALLOWED)
        assertThat(body.resultMsg).contains("POST")
    }

    @Test
    @DisplayName("handleMethodNotSupported - 비-SAP path 는 ApiResponse 형식 (기존 컨벤션 유지)")
    fun handleMethodNotSupported_nonSapPathReturnsApiResponse() {
        val ex = HttpRequestMethodNotSupportedException("GET", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/admin/auth/login"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
        val body = response.body as ApiResponse<*>
        assertThat(body.success).isFalse()
        assertThat(body.error?.code).isEqualTo("METHOD_NOT_ALLOWED")
    }

    @Test
    @DisplayName("handleMethodNotSupported - 모바일 path 도 ApiResponse 형식")
    fun handleMethodNotSupported_mobilePathReturnsApiResponse() {
        val ex = HttpRequestMethodNotSupportedException("DELETE", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("DELETE", "/api/v1/mobile/auth/login"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        val body = response.body as ApiResponse<*>
        assertThat(body.error?.code).isEqualTo("METHOD_NOT_ALLOWED")
    }

    @Test
    @DisplayName("handleBusinessException - 5xx(서버 결함)는 상태/에러코드를 그대로 응답하고 스택트레이스까지 error 로 남긴다")
    fun handleBusinessException_serverError() {
        val ex = BusinessException(
            errorCode = "STORAGE_WRITE_FAILED",
            message = "스토리지 저장에 실패했습니다",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            cause = RuntimeException("s3 put failed")
        )
        val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/v1/admin/notices/images/inline"))

        val response = handler.handleBusinessException(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body as ApiResponse<*>
        assertThat(body.success).isFalse()
        assertThat(body.error?.code).isEqualTo("STORAGE_WRITE_FAILED")
        assertThat(body.error?.message).isEqualTo("스토리지 저장에 실패했습니다")

        // serverFault 기본값(= 5xx) 이므로 error + 스택트레이스.
        assertThat(ex.serverFault).isTrue()
        val event = singleLogEvent()
        assertThat(event.level).isEqualTo(Level.ERROR)
        assertThat(event.throwableProxy).isNotNull()
    }

    @Test
    @DisplayName("handleBusinessException - 4xx(클라이언트 오류)는 스택 없이 warn 요약만 남긴다")
    fun handleBusinessException_clientError() {
        val ex = BusinessException(
            errorCode = "INVALID_PARAMETER",
            message = "잘못된 요청",
            httpStatus = HttpStatus.BAD_REQUEST
        )
        val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/v1/admin/notices"))

        val response = handler.handleBusinessException(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val body = response.body as ApiResponse<*>
        assertThat(body.error?.code).isEqualTo("INVALID_PARAMETER")

        assertThat(ex.serverFault).isFalse()
        val event = singleLogEvent()
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.throwableProxy).isNull()
    }

    @Test
    @DisplayName("handleBusinessException - 5xx 이지만 serverFault=false 면 스택 없이 warn 요약만 남긴다 (SAP 취소 거부)")
    fun handleBusinessException_serverFaultFalseLogsWarnWithoutStack() {
        // SAP 가 HTTP 200 + resultCode='E' 로 취소를 거부한 실사례 — 502 지만 서버 결함이 아니다.
        val ex = OrderCancelSapFailedException(
            detail = "판매문서가 릴리즈중입니다. 1분 후에 시도해주시길 바랍니다.",
            rejected = true,
        )
        val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/v1/mobile/orders/1/cancel"))

        val response = handler.handleBusinessException(ex, request)

        // 응답 계약(상태/코드/SAP 원문 메시지)은 종전 그대로 — 사용자 인지 경로 유지.
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
        val body = response.body as ApiResponse<*>
        assertThat(body.error?.code).isEqualTo("ORD_CANCEL_SAP_FAILED")
        assertThat(body.error?.message).isEqualTo("판매문서가 릴리즈중입니다. 1분 후에 시도해주시길 바랍니다.")

        // 로깅만 error → warn 으로 강등되고 스택트레이스가 붙지 않는다.
        assertThat(ex.httpStatus.is5xxServerError).isTrue()
        assertThat(ex.serverFault).isFalse()
        val event = singleLogEvent()
        assertThat(event.level).isEqualTo(Level.WARN)
        assertThat(event.throwableProxy).isNull()
        assertThat(event.formattedMessage).contains("ORD_CANCEL_SAP_FAILED")
    }
}
